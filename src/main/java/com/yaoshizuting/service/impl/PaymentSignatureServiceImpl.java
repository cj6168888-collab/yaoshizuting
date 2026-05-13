package com.yaoshizuting.service.impl;

import com.yaoshizuting.service.PaymentSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class PaymentSignatureServiceImpl implements PaymentSignatureService {

    @Value("${payment.wechat.api-key:}")
    private String wechatApiKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.callback.allowed-ips:127.0.0.1,::1}")
    private String allowedIPs;

    private Set<String> allowedIPSet;

    private void initAllowedIPs() {
        if (allowedIPSet == null) {
            allowedIPSet = new HashSet<>();
            for (String allowedIP : allowedIPs.split(",")) {
                String normalized = allowedIP.trim();
                if (!normalized.isEmpty()) {
                    allowedIPSet.add(normalized);
                }
            }
        }
    }

    @Override
    public boolean verifyWechatSignature(String signature, String body, String timestamp, String nonce) {
        try {
            String message = timestamp + "\n" + nonce + "\n" + body;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(wechatApiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            boolean valid = expectedSignature.equals(signature);
            if (!valid) {
                log.warn("微信签名验证失败: expected={}, actual={}", expectedSignature, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("微信签名验证异常", e);
            return false;
        }
    }

    @Override
    public boolean verifyAlipaySignature(Map<String, String> params) {
        try {
            String sign = params.remove("sign");
            if (sign == null) {
                log.warn("支付宝签名参数缺失");
                return false;
            }

            StringBuilder sb = new StringBuilder();
            params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));
            
            String signData = sb.substring(0, sb.length() - 1);
            
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPublicKey(alipayPublicKey));
            signature.update(signData.getBytes(StandardCharsets.UTF_8));
            
            boolean valid = signature.verify(Base64.getDecoder().decode(sign));
            if (!valid) {
                log.warn("支付宝签名验证失败");
            }
            return valid;
        } catch (Exception e) {
            log.error("支付宝签名验证异常", e);
            return false;
        }
    }

    @Override
    public boolean isAllowedIP(String ipAddress) {
        initAllowedIPs();
        boolean allowed = allowedIPSet.contains(ipAddress);
        if (!allowed) {
            log.warn("IP不在白名单中: {}", ipAddress);
        }
        return allowed;
    }

    private java.security.PublicKey loadPublicKey(String publicKeyPEM) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(encoded);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
