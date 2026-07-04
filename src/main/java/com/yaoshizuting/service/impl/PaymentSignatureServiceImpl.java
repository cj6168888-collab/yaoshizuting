package com.yaoshizuting.service.impl;

import com.yaoshizuting.service.PaymentSignatureService;
import com.yaoshizuting.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSignatureServiceImpl implements PaymentSignatureService {

    private final SystemConfigService systemConfigService;

    @Value("${payment.wechat.api-key:}")
    private String wechatApiKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.callback.allowed-ips:127.0.0.1,::1}")
    private String allowedIPs;

    @Override
    public boolean verifyWechatSignature(String signature, String body, String timestamp, String nonce) {
        try {
            String apiKey = systemConfigService.getConfigValue("WECHAT_API_KEY", wechatApiKey);
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("Wechat payment API key is empty");
                return false;
            }

            String message = timestamp + "\n" + nonce + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            boolean valid = expectedSignature.equals(signature);
            if (!valid) {
                log.warn("Wechat payment signature verification failed");
            }
            return valid;
        } catch (Exception e) {
            log.error("Wechat payment signature verification error", e);
            return false;
        }
    }

    @Override
    public boolean verifyAlipaySignature(Map<String, String> params) {
        try {
            String publicKey = systemConfigService.getConfigValue("ALIPAY_PUBLIC_KEY", alipayPublicKey);
            if (publicKey == null || publicKey.isBlank()) {
                log.warn("Alipay public key is empty");
                return false;
            }

            String sign = params.remove("sign");
            if (sign == null) {
                log.warn("Alipay signature parameter is missing");
                return false;
            }

            StringBuilder sb = new StringBuilder();
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));

            if (sb.isEmpty()) {
                return false;
            }

            String signData = sb.substring(0, sb.length() - 1);
            Signature signatureVerifier = Signature.getInstance("SHA256withRSA");
            signatureVerifier.initVerify(loadPublicKey(publicKey));
            signatureVerifier.update(signData.getBytes(StandardCharsets.UTF_8));

            boolean valid = signatureVerifier.verify(Base64.getDecoder().decode(sign));
            if (!valid) {
                log.warn("Alipay signature verification failed");
            }
            return valid;
        } catch (Exception e) {
            log.error("Alipay signature verification error", e);
            return false;
        }
    }

    @Override
    public boolean isAllowedIP(String ipAddress) {
        String configuredAllowedIPs = systemConfigService.getConfigValue("PAYMENT_CALLBACK_ALLOWED_IPS", allowedIPs);
        Set<String> allowedIPSet = Arrays.stream(configuredAllowedIPs.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toSet());

        boolean allowed = allowedIPSet.contains(ipAddress);
        if (!allowed) {
            log.warn("Payment callback IP is not allowed: {}", ipAddress);
        }
        return allowed;
    }

    private PublicKey loadPublicKey(String publicKeyPEM) throws Exception {
        String normalized = publicKeyPEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
