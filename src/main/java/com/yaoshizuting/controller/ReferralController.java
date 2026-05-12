package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ReferralController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    private static final String REFERRAL_LOCK_PREFIX = "referral:lock:";
    private static final long LOCK_EXPIRE_MINUTES = 5;

    @GetMapping("/invite-qr")
    public ApiResponse<Map<String, Object>> getInviteQrData(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> qrData = new HashMap<>();
        qrData.put("parentId", user.getId());
        qrData.put("parentMobile", user.getMobile());
        qrData.put("parentNickname", user.getNickname());
        qrData.put("inviteUrl", generateInviteUrl(user.getId().toString(), user.getMobile()));

        return ApiResponse.success(qrData);
    }

    @PostMapping("/bind-parent/{parentId}")
    public ApiResponse<Map<String, Object>> bindParent(
            @PathVariable Long parentId,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        if (currentUser.getParentId() != null && currentUser.getParentId() > 0) {
            return ApiResponse.error(400, "已绑定上级关系，无法修改");
        }

        User parent = userMapper.selectById(parentId);
        if (parent == null) {
            return ApiResponse.error(404, "上级用户不存在");
        }

        if (parent.getId().equals(userId)) {
            return ApiResponse.error(400, "不能绑定自己为上级");
        }

        String parentTreePath = parent.getTreePath() != null ? parent.getTreePath() : "/0/";
        if (parentTreePath.contains("/" + userId + "/")) {
            return ApiResponse.error(400, "不能绑定下级为上级");
        }

        String lockKey = REFERRAL_LOCK_PREFIX + userId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(locked)) {
            return ApiResponse.error(429, "操作频繁，请稍后再试");
        }

        try {
            currentUser.setParentId(parentId);
            currentUser.setTreePath(parentTreePath + parent.getId() + "/");
            userMapper.updateById(currentUser);

            parent.setStoreCount((parent.getStoreCount() != null ? parent.getStoreCount() : 0) + 1);
            userMapper.updateById(parent);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("parentId", parentId);
            result.put("parentMobile", parent.getMobile());
            result.put("treePath", currentUser.getTreePath());

            log.info("绑定上级关系成功: userId={}, parentId={}", userId, parentId);
            return ApiResponse.success(result);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @PostMapping("/lock-parent")
    public ApiResponse<Map<String, Object>> lockParent(@RequestBody Map<String, Object> body) {
        String mobile = (String) body.get("mobile");
        Object parentIdObj = body.get("parentId");

        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            return ApiResponse.error(400, "手机号格式不正确");
        }

        Long parentId = null;
        if (parentIdObj instanceof Integer) {
            parentId = ((Integer) parentIdObj).longValue();
        } else if (parentIdObj instanceof Long) {
            parentId = (Long) parentIdObj;
        }

        if (parentId == null || parentId <= 0) {
            return ApiResponse.error(400, "上级ID无效");
        }

        User parent = userMapper.selectById(parentId);
        if (parent == null) {
            return ApiResponse.error(404, "上级用户不存在，请检查二维码");
        }

        String lockKey = "invite:bind:" + mobile;
        Map<String, Object> bindData = new HashMap<>();
        bindData.put("parentId", parentId);
        bindData.put("parentMobile", parent.getMobile());
        bindData.put("parentNickname", parent.getNickname());
        bindData.put("lockedAt", System.currentTimeMillis());

        redisTemplate.opsForValue().set(lockKey, bindData, 30, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("mobile", mobile);
        result.put("parentId", parentId);
        result.put("parentNickname", parent.getNickname());
        result.put("locked", true);

        log.info("锁定上级关系: mobile={}, parentId={}, parentNickname={}", mobile, parentId, parent.getNickname());
        return ApiResponse.success(result);
    }

    @GetMapping("/get-locked-parent/{mobile}")
    public ApiResponse<Map<String, Object>> getLockedParent(@PathVariable String mobile) {
        String lockKey = "invite:bind:" + mobile;
        Object data = redisTemplate.opsForValue().get(lockKey);

        if (data == null) {
            return ApiResponse.success(Map.of("locked", false));
        }

        return ApiResponse.success((Map<String, Object>) data);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                Object userIdAttr = request.getAttribute("userId");
                if (userIdAttr instanceof Long) {
                    return (Long) userIdAttr;
                }
                return null;
            }
            String token = bearerToken.substring(7);
            return jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    private String generateInviteUrl(String userId, String mobile) {
        return "https://yaoshizuting.com/register?parentId=" + userId + "&mobile=" + mobile;
    }
}
