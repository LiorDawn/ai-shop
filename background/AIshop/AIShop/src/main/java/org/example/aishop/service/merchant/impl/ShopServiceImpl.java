package org.example.aishop.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.dto.ShopDTO;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.service.merchant.ShopService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private MerchantMapper merchantMapper;

    @Override
    public void addShop(Shop shop) {
        if (!StringUtils.hasText(shop.getShopName())) {
            throw new BusinessException(400, "店铺名称不能为空");
        }
        if (shop.getMerchantId() == null) {
            throw new BusinessException(400, "商家ID不能为空");
        }
        // 检查该商家是否已有店铺
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getMerchantId, shop.getMerchantId());
        if (super.getOne(wrapper, false) != null) {
            throw new BusinessException(400, "该商家已拥有店铺");
        }
        super.save(shop);
    }

    @Override
    public void updateShop(Shop shop) {
        if (shop.getId() == null) {
            throw new BusinessException(400, "店铺ID不能为空");
        }
        Shop exist = super.getById(shop.getId());
        if (exist == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        super.updateById(shop);
    }

    @Override
    public void deleteShop(Long id) {
        Shop exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        super.removeById(id);
    }

    @Override
    public void deleteBatchShops(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的店铺");
        }
        super.removeByIds(ids);
    }

    @Override
    public void updateShopStatus(Long id, Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "状态值不合法，只能为0(关闭)或1(营业)");
        }
        Shop exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        Shop shop = new Shop();
        shop.setId(id);
        shop.setStatus(status);
        super.updateById(shop);
    }

    @Override
    public ShopDTO getShopById(Long id) {
        Shop shop = super.getById(id);
        if (shop == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        return toShopDTO(shop);
    }

    @Override
    public List<ShopDTO> listShops() {
        List<Shop> list = super.list();
        return list.stream().map(this::toShopDTO).collect(Collectors.toList());
    }

    @Override
    public Page<ShopDTO> pageShops(Integer current, Integer size, String shopName, Integer status) {
        Page<Shop> page = new Page<>(current, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(shopName), Shop::getShopName, shopName);
        wrapper.eq(status != null, Shop::getStatus, status);
        wrapper.orderByDesc(Shop::getCreateTime);
        super.page(page, wrapper);
        return (Page<ShopDTO>) page.convert(this::toShopDTO);
    }

    @Override
    public ShopDTO toShopDTO(Shop shop) {
        ShopDTO dto = new ShopDTO();
        BeanUtils.copyProperties(shop, dto);
        // 手动设置 createTime（entity 是 Date，DTO 是 String）
        if (shop.getCreateTime() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dto.setCreateTime(sdf.format(shop.getCreateTime()));
        }
        // 查询商家名称
        if (shop.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(shop.getMerchantId());
            if (merchant != null) {
                dto.setMerchantName(merchant.getMerchantName());
            }
        }
        return dto;
    }
}