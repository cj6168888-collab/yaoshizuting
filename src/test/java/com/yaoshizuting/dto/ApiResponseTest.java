package com.yaoshizuting.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void successWithDataUsesDefaultSuccessMessage() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertEquals("payload", response.getData());
    }

    @Test
    void successWithoutDataReturnsNullPayload() {
        ApiResponse<Void> response = ApiResponse.success();

        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void successWithCustomMessageUsesProvidedMessage() {
        ApiResponse<Integer> response = ApiResponse.success("created", 1);

        assertEquals(200, response.getCode());
        assertEquals("created", response.getMessage());
        assertEquals(1, response.getData());
    }

    @Test
    void errorWithMessageDefaultsToServerErrorCode() {
        ApiResponse<Void> response = ApiResponse.error("failed");

        assertEquals(500, response.getCode());
        assertEquals("failed", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorWithCodeUsesProvidedCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(409, "duplicate");

        assertEquals(409, response.getCode());
        assertEquals("duplicate", response.getMessage());
        assertNull(response.getData());
    }
}
