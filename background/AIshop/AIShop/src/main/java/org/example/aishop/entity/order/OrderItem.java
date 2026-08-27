package org.example.aishop.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long skuId;
    private Long shopId;
    private Integer num;
    private BigDecimal price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Integer getNum() { return num; }
    public void setNum(Integer num) { this.num = num; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
