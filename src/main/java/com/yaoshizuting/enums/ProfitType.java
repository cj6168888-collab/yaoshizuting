package com.yaoshizuting.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProfitType {
    DIRECT_STORE("DIRECT_STORE", "直推店铺奖励"),
    INDIRECT_STORE("INDIRECT_STORE", "间推店铺奖励"),
    AGENT_MANAGE("AGENT_MANAGE", "代理商管理培训费"),
    PARTNER_DIRECT("PARTNER_DIRECT", "直推合伙人奖励"),
    PARTNER_INDIRECT("PARTNER_INDIRECT", "间推合伙人奖励"),
    TEAM_MANAGEMENT("TEAM_MANAGEMENT", "团队管理津贴"),
    HEADQUARTER_SUPPORT_FEE("HEADQUARTER_SUPPORT_FEE", "总部培训支持费");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    ProfitType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProfitType fromCode(String code) {
        for (ProfitType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown profit type: " + code);
    }
}
