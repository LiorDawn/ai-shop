package org.example.aishop.service.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.dto.ShopDTO;
import org.example.aishop.entity.merchant.Shop;

import java.util.List;

public interface ShopService extends IService<Shop> {

    void addShop(Shop shop);

    void updateShop(Shop shop);

    void deleteShop(Long id);

    void deleteBatchShops(List<Long> ids);

    void updateShopStatus(Long id, Integer status);

    ShopDTO getShopById(Long id);

    List<ShopDTO> listShops();

    IPage<ShopDTO> pageShops(Integer current, Integer size, String shopName, Integer status);

    ShopDTO toShopDTO(Shop shop);
}