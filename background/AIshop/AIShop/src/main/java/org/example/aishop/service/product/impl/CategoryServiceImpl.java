package org.example.aishop.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.CategoryTreeDTO;
import org.example.aishop.entity.product.Category;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.product.CategoryMapper;
import org.example.aishop.service.product.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void addCategory(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSort() == null) {
            // 自动计算最大排序值 + 1，新分类排到最后
            LambdaQueryWrapper<Category> maxSortWrapper = new LambdaQueryWrapper<>();
            maxSortWrapper.orderByDesc(Category::getSort).last("limit 1");
            Category maxCategory = super.getOne(maxSortWrapper, false);
            category.setSort(maxCategory != null ? maxCategory.getSort() + 1 : 1);
        }
        // 校验名称不重复
        LambdaQueryWrapper<Category> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(Category::getName, category.getName());
        if (super.count(nameCheck) > 0) {
            throw new BusinessException(400, "分类名称已存在");
        }
        super.save(category);
        deleteCategoryCache();
    }

    @Override
    public void updateCategory(Category category) {
        if (category.getId() == null) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        Category exist = super.getById(category.getId());
        if (exist == null) {
            throw new BusinessException(404, "分类不存在");
        }
        // 校验名称不重复（排除自己）
        LambdaQueryWrapper<Category> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(Category::getName, category.getName()).ne(Category::getId, category.getId());
        if (super.count(nameCheck) > 0) {
            throw new BusinessException(400, "分类名称已存在");
        }
        super.updateById(category);
        deleteCategoryCache();
    }

    @Override
    public void deleteCategory(Long id) {
        Category exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "分类不存在");
        }
        // 检查是否有子分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, id);
        if (super.count(wrapper) > 0) {
            throw new BusinessException(400, "该分类下有子分类，无法删除");
        }
        super.removeById(id);
        deleteCategoryCache();
    }

    @Override
    public List<Category> listCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        return super.list(wrapper);
    }

    @Override
    public List<CategoryTreeDTO> listTree() {
        // 1. 先查 Redis 缓存
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(RedisConstant.CATEGORY_TREE_KEY);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, new TypeReference<List<CategoryTreeDTO>>() {});
            }
        } catch (Exception ignored) {}

        // 2. 缓存未命中，查询数据库
        List<Category> all = listCategories();
        // 获取顶级分类 (parentId == 0)
        List<CategoryTreeDTO> tree = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
                .map(this::toTreeDTO)
                .collect(Collectors.toList());
        // 递归填充子分类
        for (CategoryTreeDTO parent : tree) {
            fillChildren(parent, all);
        }

        // 3. 存入 Redis
        try {
            String json = objectMapper.writeValueAsString(tree);
            stringRedisTemplate.opsForValue().set(RedisConstant.CATEGORY_TREE_KEY, json,
                    RedisConstant.CATEGORY_TREE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        return tree;
    }

    /**
     * 删除分类树缓存
     */
    private void deleteCategoryCache() {
        try {
            stringRedisTemplate.delete(RedisConstant.CATEGORY_TREE_KEY);
        } catch (Exception ignored) {}
    }

    private void fillChildren(CategoryTreeDTO parent, List<Category> all) {
        List<CategoryTreeDTO> children = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId().equals(parent.getId()))
                .map(this::toTreeDTO)
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (CategoryTreeDTO child : children) {
                fillChildren(child, all);
            }
        }
    }

    private CategoryTreeDTO toTreeDTO(Category category) {
        CategoryTreeDTO dto = new CategoryTreeDTO();
        BeanUtils.copyProperties(category, dto);
        dto.setChildren(new ArrayList<>());
        return dto;
    }
}