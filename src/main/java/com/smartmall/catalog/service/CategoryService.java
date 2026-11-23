package com.smartmall.catalog.service;

import com.smartmall.catalog.domain.Category;
import com.smartmall.catalog.vo.CategoryNode;

import java.util.List;

public interface CategoryService {

    /** 组树：一次查全表，内存分组递归。结果写入 category:tree 缓存。 */
    List<CategoryNode> tree();

    /** 面包屑：沿 parentId 向上追溯再反转拼接，如「数码家电 / 手机通讯」；无分类时返回空串。 */
    String pathOf(Long categoryId);

    /** 收集某个分类自身 + 全部子孙分类的 ID，用于按一级分类检索其下所有商品。 */
    List<Long> subtreeIds(Long categoryId);

    /** 按 ID 查分类实体，查不到抛 12005。 */
    Category getCategory(Long categoryId);

    /** 清空分类相关缓存（分类树 + 商品详情），分类增删改后调用。 */
    void evictAll();
}
