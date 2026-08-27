package org.example.aishop.entity.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    private Long shopId;
    private BigDecimal price;
    private String image;
    private Integer status;
    private String description;
    private Date createTime;
    private Integer viewPoint;
    private Integer sales;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getViewPoint() { return viewPoint; }
    public void setViewPoint(Integer viewPoint) { this.viewPoint = viewPoint; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
}