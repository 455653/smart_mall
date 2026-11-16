package com.smartmall.catalog.controller;

import com.smartmall.catalog.service.CategoryService;
import com.smartmall.catalog.vo.CategoryNode;
import com.smartmall.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor  // 构造器注入
public class CategoryController {

    private final CategoryService categoryService;

    /** 查询商品完整分类树 */
    @GetMapping("/tree")
    public Result<List<CategoryNode>> getTree() {
        return Result.ok(categoryService.tree());
    }
}
