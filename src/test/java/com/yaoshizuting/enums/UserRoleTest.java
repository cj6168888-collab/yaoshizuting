package com.yaoshizuting.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRoleTest {

    @Test
    void fromCodeReturnsEveryPersistedRole() {
        Map<Integer, UserRole> expectedRoles = Map.of(
                0, UserRole.MEMBER,
                1, UserRole.STORE,
                2, UserRole.AGENT,
                3, UserRole.PARTNER,
                9, UserRole.ADMIN,
                10, UserRole.SUPER_ADMIN);

        expectedRoles.forEach((code, role) -> assertEquals(role, UserRole.fromCode(code)));
    }

    @Test
    void roleMetadataIsExposedForPersistenceAndApiSerialization() {
        assertEquals(0, UserRole.MEMBER.getCode());
        assertEquals("会员", UserRole.MEMBER.getDesc());
        assertEquals(1, UserRole.STORE.getCode());
        assertEquals("店主", UserRole.STORE.getDesc());
        assertEquals(2, UserRole.AGENT.getCode());
        assertEquals("代理", UserRole.AGENT.getDesc());
        assertEquals(3, UserRole.PARTNER.getCode());
        assertEquals("合伙人", UserRole.PARTNER.getDesc());
        assertEquals(9, UserRole.ADMIN.getCode());
        assertEquals("管理员", UserRole.ADMIN.getDesc());
        assertEquals(10, UserRole.SUPER_ADMIN.getCode());
        assertEquals("超级管理员", UserRole.SUPER_ADMIN.getDesc());
    }

    @Test
    void fromCodeRejectsUnknownRole() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UserRole.fromCode(99));

        assertEquals("Unknown role code: 99", exception.getMessage());
    }
}
