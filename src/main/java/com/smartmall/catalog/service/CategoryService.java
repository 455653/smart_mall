package com.smartmall.catalog.service;

import com.smartmall.catalog.vo.CategoryNode;

import java.util.List;

public interface CategoryService {

    /** 组树：一次查全表，内存分组递归。 */
    List<CategoryNode> tree();
}
