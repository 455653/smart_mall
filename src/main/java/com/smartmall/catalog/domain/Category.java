package com.smartmall.catalog.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * 商品分类。自关联树（parent_id = 0 为一级），层级最多三级，不做嵌套查询，靠 level 控制。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("pms_category")
public class Category extends BaseEntity {

    /** 父分类 ID，0 表示一级分类；不用 NULL 是为了让唯一索引与排序都更简单 */
    private Long parentId = 0L;

    /** 分类名，同一父级下展示名不应重复，由运营保证 */
    private String name;

    /** 分类图标，仅一级分类在前台导航展示 */
    private String icon;

    /** 同层级排序权重，值越小越靠前 */
    private int sort;

    /** 层级 1/2/3，冗余存储省掉逐层向上递归，面包屑也靠它 */
    private int level = 1;

    /** 是否一级分类，前台楼层导航只取这一类 */
    public boolean isRoot() {
        return parentId == null || parentId == 0L;
    }

    /** 是否末级分类，只有末级才允许挂商品，避免商品挂在中间节点导致筛选混乱 */
    public boolean isLeaf() {
        return level >= 3;
    }



}
