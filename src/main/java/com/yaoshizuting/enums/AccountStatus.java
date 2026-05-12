package com.yaoshizuting.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountStatus {
    NORMAL(1, "正常"),
    FROZEN(0, "冻结");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    public static AccountStatus fromCode(int code) {
        for (AccountStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown account status: " + code);
    }
}
