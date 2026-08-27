package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "优惠券信息")
public class CouponDTO {
    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型：1=满减，2=折扣")
    private Integer type;

    @Schema(description = "最低消费金额")
    private BigDecimal minPrice;

    @Schema(description = "减免金额/折扣")
    private BigDecimal discount;

    @Schema(description = "总库存")
    private Integer stock;

    @Schema(description = "剩余数量")
    private Integer remain;

    @Schema(description = "是否已领取")
    private Boolean claimed;

    @Schema(description = "生效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    @Schema(description = "状态：0=禁用，1=启用")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getRemain() { return remain; }
    public void setRemain(Integer remain) { this.remain = remain; }
    public Boolean getClaimed() { return claimed; }
    public void setClaimed(Boolean claimed) { this.claimed = claimed; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}