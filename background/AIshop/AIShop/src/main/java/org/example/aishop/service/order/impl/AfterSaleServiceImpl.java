package org.example.aishop.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.dto.AuditRequestDTO;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.entity.order.AfterSale;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.message.AfterSaleMQMessage;
import org.example.aishop.mq.producer.AfterSaleMqProducer;
import org.example.aishop.service.order.AfterSaleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AfterSaleServiceImpl extends ServiceImpl<AfterSaleMapper, AfterSale> implements AfterSaleService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AfterSaleMqProducer afterSaleMqProducer;

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Override
    public Page<AfterSaleDTO> pageList(Integer current, Integer size,
                                       String orderNo, Integer auditStatus,
                                       Date startTime, Date endTime) {
        Page<AfterSaleDTO> page = new Page<>(current, size);
        return afterSaleMapper.selectAfterSalePage(page, orderNo, auditStatus, null, startTime, endTime, null, null);
    }

    @Override
    public AfterSaleDetailDTO getDetail(Long id) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }

        AfterSaleDetailDTO dto = new AfterSaleDetailDTO();
        BeanUtils.copyProperties(as, dto);

        // 类型/状态文字
        Integer type = as.getType();
        dto.setTypeText(type == null || type == 0 ? "仅退款" : type == 1 ? "退货退款" : "换货");
        Integer st = as.getAuditStatus();
        if (st == null || st == 0) dto.setStatusText("待审核");
        else if (st == 1) dto.setStatusText("已通过");
        else if (st == 2) dto.setStatusText("已驳回");
        else if (st == 3) dto.setStatusText("待退货");
        else if (st == 4) dto.setStatusText("已完成");

        // 订单信息
        Orders order = orderMapper.selectById(as.getOrderId());
        if (order != null) {
            dto.setOrderNo(order.getOrderNo());
            dto.setOrderCreateTime(order.getCreateTime());
            dto.setOrderTotalPrice(order.getActualPrice());
            User user = userMapper.selectById(order.getUserId());
            dto.setUsername(user != null ? user.getUsername() : "未知");
        }

        // 商品明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, as.getOrderId()));
        List<AfterSaleDetailDTO.OrderItemDTO> itemDTOs = items.stream().map(item -> {
            AfterSaleDetailDTO.OrderItemDTO iDTO = new AfterSaleDetailDTO.OrderItemDTO();
            BeanUtils.copyProperties(item, iDTO);
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                iDTO.setProductName(product.getName());
                iDTO.setProductImage(product.getImage());
            }
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                iDTO.setSpec(sku != null ? sku.getSpec() : "");
            }
            return iDTO;
        }).collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    @Override
    public void audit(AuditRequestDTO dto, String auditBy) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "售后单ID不能为空");
        }
        if (dto.getAuditStatus() == null || (dto.getAuditStatus() != 1 && dto.getAuditStatus() != 2)) {
            throw new BusinessException(400, "审核结果不合法");
        }
        if (dto.getAuditRemark() == null || dto.getAuditRemark().isEmpty()) {
            throw new BusinessException(400, "审核备注不能为空");
        }

        AfterSale as = super.getById(dto.getId());
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        if (as.getAuditStatus() != 0) {
            throw new BusinessException(400, "该售后单已审核，不能重复审核");
        }

        as.setAuditStatus(dto.getAuditStatus());
        as.setAuditRemark(dto.getAuditRemark());
        as.setAuditBy(auditBy);
        as.setAuditTime(new Date());
        // 如果通过且类型是仅退款，直接标记完成
        if (dto.getAuditStatus() == 1 && as.getType() != null && as.getType() == 0) {
            as.setAuditStatus(4); // 已完成
            as.setFinishTime(new Date());
        }
        super.updateById(as);

        // 异步通知用户审核结果
        try {
            String content = dto.getAuditStatus() == 1 ? "您的售后申请已通过" : "您的售后申请已被驳回：" + dto.getAuditRemark();
            afterSaleMqProducer.sendNotify(AfterSaleMQMessage.notify(as.getId(), as.getUserId(), as.getShopId(), content));
        } catch (Exception ignored) {}

        // 退货退款审核通过 → 投递延时消息，7天超时未填物流自动关闭
        if (dto.getAuditStatus() == 1 && as.getType() != null && as.getType() == 1) {
            try {
                afterSaleMqProducer.sendDelayClose(AfterSaleMQMessage.delayClose(as.getId(), as.getOrderId(), as.getShopId(), as.getType()));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void finish(Long id) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        if (as.getAuditStatus() != 1 && as.getAuditStatus() != 3) {
            throw new BusinessException(400, "当前状态不可标记完成");
        }
        as.setAuditStatus(4);
        as.setFinishTime(new Date());
        super.updateById(as);

        // 异步数据更新 + 通知
        try {
            afterSaleMqProducer.sendNotify(AfterSaleMQMessage.dataUpdate(as.getId(), as.getOrderId(), as.getShopId()));
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, Long orderId, Long orderItemId, Integer type,
                      BigDecimal amount, String reason, String description, String images) {
        // 校验订单是否存在且属于当前用户
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(400, "订单不存在或不属于当前用户");
        }

        // 校验订单项
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException(400, "订单项不存在");
        }

        // 获取商品所属店铺
        Long shopId = null;
        if (item.getProductId() != null) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                shopId = product.getShopId();
            }
        }

        AfterSale as = new AfterSale();
        as.setOrderId(orderId);
        as.setOrderItemId(orderItemId);
        as.setUserId(userId);
        as.setShopId(shopId);
        as.setProductId(item.getProductId());
        as.setType(type);
        as.setAmount(amount);
        as.setReason(reason);
        as.setDescription(description);
        as.setImages(images);
        as.setAuditStatus(0);
        as.setDelFlag(0);
        super.save(as);

        // 异步通知商家有新的售后申请
        try {
            afterSaleMqProducer.sendNotify(AfterSaleMQMessage.notify(as.getId(), userId, shopId, "用户发起售后申请，请及时处理"));
        } catch (Exception ignored) {}
    }

    @Override
    public Page<AfterSaleDTO> pageMyAfterSales(Long userId, Integer current, Integer size,
                                               Integer auditStatus) {
        Page<AfterSaleDTO> page = new Page<>(current, size);
        return afterSaleMapper.selectAfterSalePage(page, null, auditStatus, null, null, null, null, userId);
    }

    @Override
    public AfterSaleDetailDTO getMyDetail(Long userId, Long id) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        // 校验所有权
        Orders order = orderMapper.selectById(as.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看此售后单");
        }
        return buildDetailDTO(as);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        Orders order = orderMapper.selectById(as.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此售后单");
        }
        if (as.getAuditStatus() != 0) {
            throw new BusinessException(400, "仅待处理状态的售后单可撤销");
        }
        as.setDelFlag(1);
        super.updateById(as);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnLogistics(Long userId, Long id, String expressCompany, String expressNo) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        Orders order = orderMapper.selectById(as.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此售后单");
        }
        if (as.getAuditStatus() != 1) {
            throw new BusinessException(400, "当前状态不可填写退货物流");
        }
        if (as.getType() == null || as.getType() != 1) {
            throw new BusinessException(400, "仅退货退款类型可填写物流");
        }
        as.setExpressCompany(expressCompany);
        as.setExpressNo(expressNo);
        as.setAuditStatus(3); // 待退货 → 已发货待商家确认
        super.updateById(as);
    }

    @Override
    public Page<AfterSaleDTO> merchantPageList(Long shopId, Integer current, Integer size,
                                               String orderNo, Integer auditStatus, Integer type,
                                               Date startTime, Date endTime) {
        Page<AfterSaleDTO> page = new Page<>(current, size);
        return afterSaleMapper.selectAfterSalePage(page, orderNo, auditStatus, type, startTime, endTime, shopId, null);
    }

    @Override
    public AfterSaleDetailDTO merchantGetDetail(Long shopId, Long id) {
        AfterSale as = super.getById(id);
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        if (!shopId.equals(as.getShopId())) {
            throw new BusinessException(403, "无权查看此售后单");
        }
        return buildDetailDTO(as);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantAudit(AuditRequestDTO dto, String auditBy) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "售后单ID不能为空");
        }
        if (dto.getAuditStatus() == null || (dto.getAuditStatus() != 1 && dto.getAuditStatus() != 2)) {
            throw new BusinessException(400, "审核结果不合法");
        }

        AfterSale as = super.getById(dto.getId());
        if (as == null || as.getDelFlag() == 1) {
            throw new BusinessException(404, "售后单不存在");
        }
        if (as.getAuditStatus() != 0) {
            throw new BusinessException(400, "该售后单已处理，不能重复处理");
        }

        as.setAuditStatus(dto.getAuditStatus());
        as.setAuditRemark(dto.getAuditRemark());
        as.setAuditBy(auditBy);
        as.setAuditTime(new Date());

        if (dto.getAuditStatus() == 1) {
            // 同意
            if (as.getType() != null && as.getType() == 0) {
                // 仅退款 → 直接完成
                as.setAuditStatus(4);
                as.setFinishTime(new Date());
            } else {
                // 退货退款 → 需要用户退货
                if (dto.getReturnAddress() != null && !dto.getReturnAddress().isEmpty()) {
                    as.setReturnAddress(dto.getReturnAddress());
                }
                // 状态保持为1（已通过/待退货）
            }
        }
        super.updateById(as);
    }

    // ---------- 私有辅助方法 ----------

    /**
     * 构建完整的售后详情DTO
     */
    private AfterSaleDetailDTO buildDetailDTO(AfterSale as) {
        AfterSaleDetailDTO dto = new AfterSaleDetailDTO();
        BeanUtils.copyProperties(as, dto);

        // 类型/状态文字
        Integer type = as.getType();
        dto.setTypeText(type == null || type == 0 ? "仅退款" : type == 1 ? "退货退款" : "换货");
        Integer st = as.getAuditStatus();
        if (st == null || st == 0) dto.setStatusText("待处理");
        else if (st == 1) dto.setStatusText("商家已同意");
        else if (st == 2) dto.setStatusText("已拒绝");
        else if (st == 3) dto.setStatusText("待退货");
        else if (st == 4) dto.setStatusText("退款完成");

        // 订单信息
        Orders order = orderMapper.selectById(as.getOrderId());
        if (order != null) {
            dto.setOrderNo(order.getOrderNo());
            dto.setOrderCreateTime(order.getCreateTime());
            dto.setOrderTotalPrice(order.getActualPrice());
            User user = userMapper.selectById(order.getUserId());
            dto.setUsername(user != null ? user.getUsername() : "未知");
        }

        // 商品明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, as.getOrderId()));
        List<AfterSaleDetailDTO.OrderItemDTO> itemDTOs = items.stream().map(item -> {
            AfterSaleDetailDTO.OrderItemDTO iDTO = new AfterSaleDetailDTO.OrderItemDTO();
            BeanUtils.copyProperties(item, iDTO);
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                iDTO.setProductName(product.getName());
                iDTO.setProductImage(product.getImage());
            }
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                ProductSku sku = productSkuMapper.selectById(item.getSkuId());
                iDTO.setSpec(sku != null ? sku.getSpec() : "");
            }
            return iDTO;
        }).collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }
}