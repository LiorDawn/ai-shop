package org.example.aishop.service.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.dto.AuditRequestDTO;

import java.math.BigDecimal;
import java.util.Date;

public interface AfterSaleService {
    Page<AfterSaleDTO> pageList(Integer current, Integer size,
                                String orderNo, Integer auditStatus,
                                Date startTime, Date endTime);

    AfterSaleDetailDTO getDetail(Long id);

    void audit(AuditRequestDTO dto, String auditBy);

    void finish(Long id);

    /**
     * 用户端：申请售后
     */
    void apply(Long userId, Long orderId, Long orderItemId, Integer type,
               BigDecimal amount, String reason, String description, String images);

    /**
     * 用户端：查询我的售后列表
     */
    Page<AfterSaleDTO> pageMyAfterSales(Long userId, Integer current, Integer size,
                                        Integer auditStatus);

    /**
     * 用户端：查询售后详情
     */
    AfterSaleDetailDTO getMyDetail(Long userId, Long id);

    /**
     * 用户端：撤销售后
     */
    void cancel(Long userId, Long id);

    /**
     * 用户端：填写退货物流
     */
    void returnLogistics(Long userId, Long id, String expressCompany, String expressNo);

    /**
     * 商家端：售后列表（按店铺）
     */
    Page<AfterSaleDTO> merchantPageList(Long shopId, Integer current, Integer size,
                                        String orderNo, Integer auditStatus, Integer type,
                                        Date startTime, Date endTime);

    /**
     * 商家端：售后详情
     */
    AfterSaleDetailDTO merchantGetDetail(Long shopId, Long id);

    /**
     * 商家端：处理售后
     */
    void merchantAudit(AuditRequestDTO dto, String auditBy);
}