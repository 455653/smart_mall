package com.smartmall.catalog.vo;


import com.smartmall.catalog.domain.Category;

import java.util.List;

/**
 * 分类树节点：一个分类 + 它的子分类。
 *
 * <p>用 record 而不是可变实体：节点一旦放进缓存就不该再被改，
 * 每次组树都造新对象，缓存里的旧对象不会受影响。
 *
 * @param id       分类 ID
 * @param parentId 父分类 ID，一级为 0
 * @param name     分类名
 * @param icon     图标           递归算法  层数少  三层
 * @param level    层级 1/2/3
 * @param sort     排序权重
 * @param children 子分类，叶子节点为空列表
 *
 *
 *                  点一层 查一层    多层级
 */
public record CategoryNode(Long id, Long parentId, String name, String icon,
                           int level, int sort, List<CategoryNode> children) {

    /**
     * 由实体 + 子节点构造。
     *
     * @param category 分类实体
     * @param children 子节点列表，传 null 当作没有子节点
     */
    public static CategoryNode of(Category category, List<CategoryNode> children) {
        // 把实体字段逐个搬过来，children 为 null 时兜底成空列表（前端不用判空）
        return new CategoryNode(category.getId(), category.getParentId(), category.getName(),
                category.getIcon(), category.getLevel(), category.getSort(),
                children == null ? List.of() : children);
    }
}
