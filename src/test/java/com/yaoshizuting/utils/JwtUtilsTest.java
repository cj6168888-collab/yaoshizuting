package com.yaoshizuting.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class JwtUtilsTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", 60_000L);
    }

    @Test
    void generateTokenIncludesUserClaims() {
        String token = jwtUtils.generateToken(10L, "13800138000", 2);

        Claims claims = jwtUtils.parseToken(token);

        assertEquals("10", claims.getSubject());
        assertEquals(10L, jwtUtils.getUserIdFromToken(token));
        assertEquals("13800138000", jwtUtils.getMobileFromToken(token));
        assertEquals(2, jwtUtils.getRoleFromToken(token));
        assertFalse(jwtUtils.isTokenExpired(token));
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void invalidTokenIsExpiredAndNotValid() {
        assertTrue(jwtUtils.isTokenExpired("not-a-jwt"));
        assertFalse(jwtUtils.validateToken("not-a-jwt"));
    }

    @Test
    void expiredTokenIsParsedButNotValid() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", -1_000L);
        String token = jwtUtils.generateToken(20L, "13900139000", 3);

        assertTrue(jwtUtils.isTokenExpired(token));
        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseWhenExpirationCheckFailsAfterParsing() {
        JwtUtils spyJwtUtils = spy(jwtUtils);
        Claims claims = mock(Claims.class);
        doReturn(claims).when(spyJwtUtils).parseToken("parsed-token");
        doReturn(true).when(spyJwtUtils).isTokenExpired("parsed-token");

        assertFalse(spyJwtUtils.validateToken("parsed-token"));
    }
}
