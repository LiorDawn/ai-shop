package org.example.aishop.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import org.example.aishop.dto.CategoryTreeDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.product.Category;
import org.example.aishop.service.product.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result<Void> add(@RequestBody Category category) {
        categoryService.addCategory(category);
        return Result.success("添加成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        categoryService.updateCategory(category);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }

    @GetMapping("/list")
    public Result<List<Category>> list() {
        List<Category> list = categoryService.listCategories();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "分类树形结构", description = "返回父子层级嵌套的分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeDTO>> tree() {
        List<CategoryTreeDTO> tree = categoryService.listTree();
        return Result.success("查询成功", tree);
    }
}