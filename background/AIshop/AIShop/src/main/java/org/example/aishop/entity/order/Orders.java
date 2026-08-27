package org.example.aishop.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalPrice;
    private BigDecimal couponPrice;
    private BigDecimal actualPrice;
    private Integer payType;
    private Integer payStatus;
    private Integer orderStatus;
    private Long addressId;
    private String logistics;
    private String remark;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public BigDecimal getCouponPrice() { return couponPrice; }
    public void setCouponPrice(BigDecimal couponPrice) { this.couponPrice = couponPrice; }
    public BigDecimal getActualPrice() { return actualPrice; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    public Integer getPayType() { return payType; }
    public void setPayType(Integer payType) { this.payType = payType; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getLogistics() { return logistics; }
    public void setLogistics(String logistics) { this.logistics = logistics; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
