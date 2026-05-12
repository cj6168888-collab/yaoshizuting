package com.yaoshizuting.service;

import java.util.Map;

public interface PaymentSignatureService {
    
    boolean verifyWechatSignature(String signature, String body, String timestamp, String nonce);
    
    boolean verifyAlipaySignature(Map<String, String> params);
    
    boolean isAllowedIP(String ipAddress);
}
