package com.yaoshizuting.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfitTypeTest {

    @Test
    void fromCodeReturnsEveryPersistedProfitType() {
        Map<String, ProfitType> expectedTypes = Map.of(
                "DIRECT_STORE", ProfitType.DIRECT_STORE,
                "INDIRECT_STORE", ProfitType.INDIRECT_STORE,
                "AGENT_MANAGE", ProfitType.AGENT_MANAGE,
                "PARTNER_DIRECT", ProfitType.PARTNER_DIRECT,
                "PARTNER_INDIRECT", ProfitType.PARTNER_INDIRECT,
                "TEAM_MANAGEMENT", ProfitType.TEAM_MANAGEMENT,
                "HEADQUARTER_SUPPORT_FEE", ProfitType.HEADQUARTER_SUPPORT_FEE);

        expectedTypes.forEach((code, type) -> assertEquals(type, ProfitType.fromCode(code)));
    }

    @Test
    void profitTypeMetadataIsExposedForPersistenceAndApiSerialization() {
        assertEquals("DIRECT_STORE", ProfitType.DIRECT_STORE.getCode());
        assertEquals("直推店铺奖励", ProfitType.DIRECT_STORE.getDesc());
        assertEquals("INDIRECT_STORE", ProfitType.INDIRECT_STORE.getCode());
        assertEquals("间推店铺奖励", ProfitType.INDIRECT_STORE.getDesc());
        assertEquals("AGENT_MANAGE", ProfitType.AGENT_MANAGE.getCode());
        assertEquals("代理商管理培训费", ProfitType.AGENT_MANAGE.getDesc());
        assertEquals("PARTNER_DIRECT", ProfitType.PARTNER_DIRECT.getCode());
        assertEquals("直推合伙人奖励", ProfitType.PARTNER_DIRECT.getDesc());
        assertEquals("PARTNER_INDIRECT", ProfitType.PARTNER_INDIRECT.getCode());
        assertEquals("间推合伙人奖励", ProfitType.PARTNER_INDIRECT.getDesc());
        assertEquals("TEAM_MANAGEMENT", ProfitType.TEAM_MANAGEMENT.getCode());
        assertEquals("团队管理津贴", ProfitType.TEAM_MANAGEMENT.getDesc());
        assertEquals("HEADQUARTER_SUPPORT_FEE", ProfitType.HEADQUARTER_SUPPORT_FEE.getCode());
        assertEquals("总部培训支持费", ProfitType.HEADQUARTER_SUPPORT_FEE.getDesc());
    }

    @Test
    void fromCodeRejectsUnknownProfitType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProfitType.fromCode("UNKNOWN"));

        assertEquals("Unknown profit type: UNKNOWN", exception.getMessage());
    }
}
