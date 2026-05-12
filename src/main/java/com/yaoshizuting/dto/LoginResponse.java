package com.yaoshizuting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录成功返回信息")
public class LoginResponse {
    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "用户 ID", example = "10001")
    private Long userId;

    @Schema(description = "用户角色：0普通会员，1店铺会员，2代理会员，3合伙人，9管理员", example = "9")
    private Integer role;

    @Schema(description = "用户昵称", example = "药师祖庭管理员")
    private String nickname;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "头像地址", example = "/uploads/avatar/default.png")
    private String avatar;

    @Schema(description = "上级用户 ID", example = "10000")
    private Long parentId;

    @Schema(description = "团队树路径", example = "/10000/10001/")
    private String treePath;
}
