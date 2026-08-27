package org.example.aishop.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.entity.order.AfterSale;

import java.util.Date;

public interface AfterSaleMapper extends BaseMapper<AfterSale> {

    /** 售后列表（关联订单、用户、订单项、商品、SKU） */
    Page<AfterSaleDTO> selectAfterSalePage(Page<AfterSaleDTO> page,
                                           @Param("orderNo") String orderNo,
                                           @Param("auditStatus") Integer auditStatus,
                                           @Param("type") Integer type,
                                           @Param("startTime") Date startTime,
                                           @Param("endTime") Date endTime,
                                           @Param("shopId") Long shopId,
                                           @Param("userId") Long userId);
}