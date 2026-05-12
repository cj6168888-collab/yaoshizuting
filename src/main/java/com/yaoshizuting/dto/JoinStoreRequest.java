package com.yaoshizuting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinStoreRequest {
    @NotNull(message = "支付方式不能为空")
    private Integer payMethod;

    private String inviteCode;
}
