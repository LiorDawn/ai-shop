package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分类树节点")
public class CategoryTreeDTO {
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父级分类ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "子分类列表")
    private List<CategoryTreeDTO> children;
}