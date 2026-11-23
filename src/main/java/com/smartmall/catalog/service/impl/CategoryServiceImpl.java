package com.smartmall.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.catalog.domain.Category;
import com.smartmall.catalog.mapper.CategoryMapper;
import com.smartmall.catalog.service.CategoryService;
import com.smartmall.catalog.vo.CategoryNode;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.config.CacheConfig;
import com.smartmall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service                     // 注册为业务组件
@RequiredArgsConstructor     // 构造器注入
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_DEPTH = 3;

    /** 面包屑向上追溯的最大层数，防止脏数据成环导致死循环 */
    private static final int MAX_PATH_DEPTH = 5;

    private final CategoryMapper categoryMapper;

    /**
     * 分类树，全站共用一份缓存。
     *
     * <p>key 用常量 {@code 'all'} 而不是方法参数：全站只有一棵树。
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.CATEGORY_TREE, key = "'all'")
    public List<CategoryNode> tree() {
        // 一次查全部分类，按 sort、id 排序，每个节点天然就是对的
        List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getSort)       // 运营配置的展示顺序优先
                .orderByAsc(Category::getId));

        // 用来装载一级分类
        List<CategoryNode> roots = new ArrayList<>();
        for (Category category : categories) {
            if (category.isRoot()) {
                roots.add(buildNode(category, categories, 1));
            }
        }
        // 遍历全部分类，挑出一级分类（parentId 为 0），各自往下挂子节点
        return roots;
    }

    /**
     * 把一个分类的子分类挂到它下面，再对子分类重复同样的动作。
     *
     * @param parent 当前分类
     * @param all    全部分类（用来找谁的父亲是 parent）
     * @param level  当前层级，从 1 开始
     */
    private CategoryNode buildNode(Category parent, List<Category> all, int level) {
        List<CategoryNode> children = new ArrayList<>();
        if (level < MAX_DEPTH) {
            for (Category child : all) {
                // 父节点的 id 和子节点的父节点 id 相等，说明找到了
                if (parent.getId().equals(child.getParentId())) {
                    children.add(buildNode(child, all, level + 1));
                }
            }
        }
        // 当前节点
        return CategoryNode.of(parent, children);
    }

    /**
     * 面包屑：沿 parentId 向上拼名称，如「数码家电 / 手机通讯」。
     *
     * <p>带 guard 计数：脏数据（A 的父是 B、B 的父是 A）会死循环，
     * 最多向上走 {@link #MAX_PATH_DEPTH} 层就收手。
     */
    @Override
    public String pathOf(Long categoryId) {
        // 没有分类 ID（商品未挂分类）时返回空串，调用方不用判空
        if (categoryId == null) {
            return "";
        }

        // 收集沿途的名称，从下往上加，最后反转
        List<String> names = new ArrayList<>();

        Long current = categoryId;
        int guard = 0;

        // 沿 parentId 向上走：current 为 null 或 0（根）时停，或超过最大层数时停
        while (current != null && current != 0L && guard++ < MAX_PATH_DEPTH) {
            Category category = categoryMapper.selectById(current);
            // 数据被删或 ID 不存在：终止追溯，返回已收集的部分
            if (category == null) {
                break;
            }
            names.add(category.getName());
            current = category.getParentId();
        }

        // 反转成从上往下的顺序（数码家电 → 手机通讯）
        Collections.reverse(names);

        return String.join(" / ", names);
    }

    /**
     * 收集自身 + 所有子孙分类 ID。
     *
     * <p>"点一级分类看全部商品"靠它：检索时 {@code in(categoryId, ids)}。
     * 用 BFS 而不是递归，队列天然按层推进，不容易栈溢出。
     */
    @Override
    public List<Long> subtreeIds(Long categoryId) {
        // 没有分类限制时返回空列表（调用方据此判断"不加分类条件"）
        if (categoryId == null) {
            return List.of();
        }

        // 一次查全表：只需要 id 与 parentId 两列，但实体查询更简单，数据量也小
        List<Category> all = categoryMapper.selectList(null);

        // 构建"父 ID → 子 ID 列表"的索引，避免在循环里反复扫描全表
        Map<Long, List<Long>> childrenOf = all.stream().collect(Collectors.groupingBy(
                c -> c.getParentId() == null ? 0L : c.getParentId(),
                Collectors.mapping(Category::getId, Collectors.toList())));

        // 结果列表：自身 + 所有子孙
        List<Long> result = new ArrayList<>();

        // BFS 队列
        Deque<Long> queue = new ArrayDeque<>();

        // 从自身开始
        queue.add(categoryId);

        // 队列不空就一直处理
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            // 收集它的 ID（自身也在内，所以直接用 in 查询能覆盖自己挂的商品）
            result.add(current);
            // 把它的所有子节点入队（没有子节点时取空列表）
            queue.addAll(childrenOf.getOrDefault(current, List.of()));
        }

        return result;
    }

    /** 单查，找不到即 12005 */
    @Override
    public Category getCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 清缓存入口：分类的增删改都要调它。
     *
     * <p>连 {@code PRODUCT_DETAIL} 一起清：商品详情里嵌了面包屑，
     * 分类改名后如果只清分类树，详情页会继续显示旧路径。
     */
    @Override
    @CacheEvict(cacheNames = {CacheConfig.CATEGORY_TREE, CacheConfig.PRODUCT_DETAIL}, allEntries = true)
    public void evictAll() {
        // 只负责清缓存，没有业务逻辑：注解在方法执行后清空两个 cache 的所有条目
    }
}
