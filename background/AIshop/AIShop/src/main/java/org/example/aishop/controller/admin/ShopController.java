package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.ShopDTO;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.service.merchant.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端店铺管理", description = "店铺CRUD、状态管理")
@RestController
@RequestMapping("/admin/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @PostMapping
    public Result<Void> add(@RequestBody Shop shop) {
        shopService.addShop(shop);
        return Result.success("添加成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Shop shop) {
        shopService.updateShop(shop);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.deleteShop(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        shopService.deleteBatchShops(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "更新店铺状态")
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        shopService.updateShopStatus(id, status);
        return Result.success("状态更新成功");
    }

    @GetMapping("/{id}")
    public Result<ShopDTO> getById(@PathVariable Long id) {
        ShopDTO dto = shopService.getShopById(id);
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "店铺列表", description = "返回所有店铺列表")
    @GetMapping("/list")
    public Result<List<ShopDTO>> list() {
        List<ShopDTO> list = shopService.listShops();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "店铺分页查询")
    @GetMapping("/page")
    public Result<Page<ShopDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) String shopName,
                                      @RequestParam(required = false) Integer status) {
        Page<ShopDTO> page = (Page<ShopDTO>) shopService.pageShops(current, size, shopName, status);
        return Result.success("查询成功", page);
    }
}