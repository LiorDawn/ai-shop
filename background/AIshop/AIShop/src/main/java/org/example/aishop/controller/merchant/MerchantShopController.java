package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.crypto.digest.BCrypt;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.ShopDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.entity.user.User;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.service.merchant.ShopService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "商家店铺管理", description = "商家店铺信息、修改密码")
@RestController
@RequestMapping("/merchant/shop")
public class MerchantShopController {

    @Autowired
    private ShopService shopService;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "获取店铺信息", description = "含商家信息、用户名")
    @GetMapping("/info")
    public Result<Map<String, Object>> getShopInfo() {
        Shop shop = getCurrentShop();
        ShopDTO dto = shopService.toShopDTO(shop);

        // 商家信息
        Merchant merchant = merchantMapper.selectById(shop.getMerchantId());
        UserDTO currentUser = UserHolder.getUser();

        Map<String, Object> data = new HashMap<>();
        data.put("shop", dto);
        data.put("merchant", merchant);
        data.put("username", currentUser != null ? currentUser.getUsername() : "");
        data.put("phone", currentUser != null ? currentUser.getPhone() : "");
        return Result.success("查询成功", data);
    }

    /** 更新店铺信息 */
    @PutMapping("/info")
    public Result<Void> updateShopInfo(@RequestBody Shop shop) {
        Shop current = getCurrentShop();
        shop.setId(current.getId());
        shop.setMerchantId(null); // 不允许修改
        shop.setStatus(null);     // 通过单独接口修改
        shop.setCreateTime(null);

        if (!StringUtils.hasText(shop.getShopName())) {
            throw new BusinessException(400, "店铺名称不能为空");
        }
        shopService.updateShop(shop);
        return Result.success("保存成功");
    }

    /** 切换营业状态 */
    @PutMapping("/status")
    public Result<Void> toggleStatus(@RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值不合法");
        }
        Shop current = getCurrentShop();
        shopService.updateShopStatus(current.getId(), status);
        return Result.success(status == 1 ? "已切换为营业中" : "已暂停营业");
    }

    @Operation(summary = "获取商家个人资料")
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(currentUser.getId());

        // 商家信息通过当前店铺的 merchantId 查询
        Shop shop = getCurrentShop();
        Merchant merchant = merchantMapper.selectById(shop.getMerchantId());

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("phone", user.getPhone());
        data.put("merchantName", merchant != null ? merchant.getMerchantName() : "");
        return Result.success("查询成功", data);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "原密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(400, "新密码长度不能少于6位");
        }

        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    // ===== 辅助方法 =====

    private Shop getCurrentShop() {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        return shop;
    }
}