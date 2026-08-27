package org.example.aishop.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.entity.user.Address;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.user.AddressMapper;
import org.example.aishop.service.user.AddressService;
import org.example.aishop.util.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public List<Address> listCurrentUserAddresses() {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        wrapper.orderByDesc(Address::getIsDefault).orderByDesc(Address::getId);
        return super.list(wrapper);
    }

    @Override
    public void addAddress(Address address) {
        Long userId = UserHolder.getUserId();
        address.setUserId(userId);
        if (!StringUtils.hasText(address.getReceiver())) {
            throw new BusinessException(400, "收件人不能为空");
        }
        if (!StringUtils.hasText(address.getPhone())) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!StringUtils.hasText(address.getAddress())) {
            throw new BusinessException(400, "地址不能为空");
        }

        // 如果设置为默认地址，先清除其他默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultFlag(userId);
        } else {
            address.setIsDefault(0);
            // 如果当前没有地址，自动设为默认
            long count = super.count(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
            if (count == 0) {
                address.setIsDefault(1);
            }
        }
        super.save(address);
    }

    @Override
    public void updateAddress(Address address) {
        if (address.getId() == null) {
            throw new BusinessException(400, "地址ID不能为空");
        }
        Address exist = super.getById(address.getId());
        if (exist == null) {
            throw new BusinessException(404, "地址不存在");
        }
        Long userId = UserHolder.getUserId();
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(403, "无权操作此地址");
        }
        address.setUserId(userId);

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultFlag(userId);
        }
        super.updateById(address);
    }

    @Override
    public void deleteAddress(Long id) {
        Address exist = super.getById(id);
        if (exist == null) return;
        Long userId = UserHolder.getUserId();
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException(403, "无权操作此地址");
        }
        super.removeById(id);
    }

    @Override
    public Address getDefaultAddress() {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        wrapper.eq(Address::getIsDefault, 1);
        return super.getOne(wrapper, false);
    }

    private void clearDefaultFlag(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId).eq(Address::getIsDefault, 1);
        List<Address> defaultList = super.list(wrapper);
        for (Address addr : defaultList) {
            addr.setIsDefault(0);
            super.updateById(addr);
        }
    }
}