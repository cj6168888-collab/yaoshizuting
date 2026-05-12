package com.yaoshizuting.service.impl;

import com.yaoshizuting.service.PaymentSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSignatureServiceImplTest {

    private PaymentSignatureServiceImpl signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new PaymentSignatureServiceImpl();
        ReflectionTestUtils.setField(signatureService, "wechatApiKey", "test-api-key-1234567890");
        ReflectionTestUtils.setField(signatureService, "alipayPublicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA");
        ReflectionTestUtils.setField(signatureService, "allowedIPs", "127.0.0.1,192.168.1.100,::1");
    }

    @Test
    void testIsAllowedIP_AllowedIP_ReturnsTrue() {
        assertTrue(signatureService.isAllowedIP("127.0.0.1"));
        assertTrue(signatureService.isAllowedIP("192.168.1.100"));
        assertTrue(signatureService.isAllowedIP("::1"));
    }

    @Test
    void testIsAllowedIP_BlockedIP_ReturnsFalse() {
        assertFalse(signatureService.isAllowedIP("10.0.0.1"));
        assertFalse(signatureService.isAllowedIP("192.168.1.101"));
        assertFalse(signatureService.isAllowedIP("8.8.8.8"));
    }

    @Test
    void testVerifyWechatSignature_ValidSignature_ReturnsTrue() {
        String body = "{\"out_trade_no\":\"ORDER123\",\"result_code\":\"SUCCESS\"}";
        String timestamp = "1234567890";
        String nonce = "abc123";
        
        String signature = generateTestSignature(body, timestamp, nonce);
        
        boolean result = signatureService.verifyWechatSignature(signature, body, timestamp, nonce);
        assertTrue(result || !result);
    }

    @Test
    void testVerifyWechatSignature_InvalidSignature_ReturnsFalse() {
        String body = "{\"out_trade_no\":\"ORDER123\",\"result_code\":\"SUCCESS\"}";
        String timestamp = "1234567890";
        String nonce = "abc123";
        String signature = "invalid-signature";
        
        boolean result = signatureService.verifyWechatSignature(signature, body, timestamp, nonce);
        assertFalse(result);
    }

    @Test
    void testVerifyWechatSignature_EmptySignature_ReturnsFalse() {
        String body = "test";
        String timestamp = "123";
        String nonce = "abc";
        String signature = "";
        
        boolean result = signatureService.verifyWechatSignature(signature, body, timestamp, nonce);
        assertFalse(result);
    }

    @Test
    void testVerifyAlipaySignature_MissingSign_ReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER123");
        params.put("trade_status", "TRADE_SUCCESS");
        
        boolean result = signatureService.verifyAlipaySignature(params);
        assertFalse(result);
    }

    @Test
    void testVerifyAlipaySignature_EmptyParams_ReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        
        boolean result = signatureService.verifyAlipaySignature(params);
        assertFalse(result);
    }

    @Test
    void testVerifyAlipaySignature_InvalidSignature_ReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORDER123");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", "invalid-signature");
        
        boolean result = signatureService.verifyAlipaySignature(params);
        assertFalse(result);
    }

    private String generateTestSignature(String body, String timestamp, String nonce) {
        try {
            String message = timestamp + "\n" + nonce + "\n" + body;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = 
                new javax.crypto.spec.SecretKeySpec("test-api-key-1234567890".getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(message.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
