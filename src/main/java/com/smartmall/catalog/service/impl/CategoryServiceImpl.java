package com.smartmall.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.catalog.domain.Category;
import com.smartmall.catalog.mapper.CategoryMapper;
import com.smartmall.catalog.service.CategoryService;
import com.smartmall.catalog.vo.CategoryNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service                     // 注册为业务组件
@RequiredArgsConstructor     // 构造器注入
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_DEPTH = 3;

    private final CategoryMapper categoryMapper;

    @Override
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
}
