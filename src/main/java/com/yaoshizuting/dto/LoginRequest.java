package com.yaoshizuting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "手机验证码登录请求")
public class LoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Schema(description = "中国大陆手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "短信验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "邀请绑定码，可选", example = "INV10001")
    private String inviteCode;
}
