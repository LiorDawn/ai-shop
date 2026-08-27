package org.example.aishop.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.entity.user.Address;

import java.util.List;

public interface AddressService extends IService<Address> {
    List<Address> listCurrentUserAddresses();
    void addAddress(Address address);
    void updateAddress(Address address);
    void deleteAddress(Long id);
    Address getDefaultAddress();
}