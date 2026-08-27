package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户信息")
public class UserDTO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别：0=未知，1=男，2=女")
    private Integer gender;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "状态：0=禁用，1=正常")
    private Integer status;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "绑定的店铺ID（商家用户）")
    private Long shopId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
}