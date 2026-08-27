package org.example.aishop.service.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.dto.MerchantDTO;
import org.example.aishop.entity.merchant.Merchant;

import java.util.List;

public interface MerchantService extends IService<Merchant> {

    void addMerchant(Merchant merchant);

    void updateMerchant(Merchant merchant);

    void deleteMerchant(Long id);

    void deleteBatchMerchants(List<Long> ids);

    void auditMerchant(Long id, Integer status, String auditRemark);

    MerchantDTO getMerchantById(Long id);

    List<MerchantDTO> listMerchants();

    IPage<MerchantDTO> pageMerchants(Integer current, Integer size, String merchantName, Integer status);

    MerchantDTO toMerchantDTO(Merchant merchant);

    /** 用户端：查询我的入驻申请状态 */
    MerchantDTO getMyApplication(Long userId);
}