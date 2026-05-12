package com.yaoshizuting.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    MEMBER(0, "会员"),
    STORE(1, "店主"),
    AGENT(2, "代理"),
    PARTNER(3, "合伙人"),
    ADMIN(9, "管理员"),
    SUPER_ADMIN(10, "超级管理员");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}
