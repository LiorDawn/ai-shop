package org.example.aishop.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.dto.CategoryTreeDTO;
import org.example.aishop.entity.product.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    void addCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(Long id);

    List<Category> listCategories();

    List<CategoryTreeDTO> listTree();
}