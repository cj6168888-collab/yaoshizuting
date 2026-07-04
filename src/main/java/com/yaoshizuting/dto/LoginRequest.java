package com.yaoshizuting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @Schema(description = "中国大陆手机号，短信登录或手机号密码登录时使用", example = "13800138000")
    private String mobile;

    @Schema(description = "短信验证码，短信登录时使用", example = "123456")
    private String code;

    @Schema(description = "用户名，账号密码登录时使用", example = "admin")
    private String username;

    @Schema(description = "密码，账号密码登录时使用")
    private String password;

    @Schema(description = "邀请绑定码，可选", example = "INV10001")
    private String inviteCode;
}
