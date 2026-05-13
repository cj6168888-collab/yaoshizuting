package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.TeamService;
import com.yaoshizuting.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralControllerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TeamService teamService;

    @Mock
    private HttpServletRequest request;

    private ReferralController controller;

    @BeforeEach
    void setUp() {
        controller = new ReferralController(redisTemplate, userMapper, jwtUtils, teamService, new ObjectMapper());
    }

    @Test
    void getInviteQrDataRejectsAnonymousRequest() {
        ApiResponse<Map<String, Object>> response = controller.getInviteQrData(request);

        assertEquals(401, response.getCode());
        assertEquals("请先登录", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void getInviteQrDataReturnsInvitePayloadForBearerToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("X-Forwarded-Host")).thenReturn("h5.yaoshizuting.com");
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(12L);
        when(userMapper.selectById(12L)).thenReturn(user(12L, "13800138000", "邀请人"));

        ApiResponse<Map<String, Object>> response = controller.getInviteQrData(request);

        assertEquals(200, response.getCode());
        assertEquals(12L, response.getData().get("parentId"));
        assertEquals("13800138000", response.getData().get("parentMobile"));
        assertEquals("邀请人", response.getData().get("parentNickname"));
        assertEquals(
                "https://h5.yaoshizuting.com/?parentId=12&mobile=13800138000&inviterName=%E9%82%80%E8%AF%B7%E4%BA%BA",
                response.getData().get("inviteUrl"));
    }

    @Test
    void getInviteQrDataReturnsNotFoundWhenUserIsMissing() {
        when(request.getAttribute("userId")).thenReturn(12L);
        when(userMapper.selectById(12L)).thenReturn(null);

        ApiResponse<Map<String, Object>> response = controller.getInviteQrData(request);

        assertEquals(404, response.getCode());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void getInviteQrDataFallsBackToRequestAttributeForNonBearerHeader() {
        when(request.getHeader("Authorization")).thenReturn("Basic credentials");
        when(request.getAttribute("userId")).thenReturn(12L);
        when(userMapper.selectById(12L)).thenReturn(user(12L, "13800138000", "邀请人"));

        ApiResponse<Map<String, Object>> response = controller.getInviteQrData(request);

        assertEquals(200, response.getCode());
        assertEquals(12L, response.getData().get("parentId"));
        verify(jwtUtils, never()).getUserIdFromToken(any());
    }

    @Test
    void getInviteQrDataTreatsTokenParsingFailureAsAnonymous() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtils.getUserIdFromToken("invalid-token")).thenThrow(new IllegalArgumentException("bad token"));

        ApiResponse<Map<String, Object>> response = controller.getInviteQrData(request);

        assertEquals(401, response.getCode());
        assertEquals("请先登录", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void bindParentRejectsAnonymousRequest() {
        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(401, response.getCode());
        assertEquals("请先登录", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void bindParentRejectsMissingCurrentUser() {
        when(request.getAttribute("userId")).thenReturn(20L);
        when(userMapper.selectById(20L)).thenReturn(null);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(404, response.getCode());
        assertEquals("用户不存在", response.getMessage());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void bindParentRejectsAlreadyBoundUser() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        currentUser.setParentId(3L);
        when(userMapper.selectById(20L)).thenReturn(currentUser);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(400, response.getCode());
        assertEquals("已绑定上级关系，无法修改", response.getMessage());
        verify(redisTemplate, never()).opsForValue();
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindParentRejectsMissingParent() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        when(userMapper.selectById(20L)).thenReturn(currentUser);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(404, response.getCode());
        assertEquals("上级用户不存在", response.getMessage());
        verify(redisTemplate, never()).opsForValue();
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindParentRejectsSelfAsParent() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        when(userMapper.selectById(20L)).thenReturn(currentUser);

        ApiResponse<Map<String, Object>> response = controller.bindParent(20L, request);

        assertEquals(400, response.getCode());
        assertEquals("不能绑定自己为上级", response.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindParentRejectsDescendantAsParent() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        User parent = user(30L, "13800138030", "下级用户");
        parent.setTreePath("/0/20/");
        when(userMapper.selectById(20L)).thenReturn(currentUser);
        when(userMapper.selectById(30L)).thenReturn(parent);

        ApiResponse<Map<String, Object>> response = controller.bindParent(30L, request);

        assertEquals(400, response.getCode());
        assertEquals("不能绑定下级为上级", response.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindParentRejectsFrequentOperationWhenLockExists() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        User parent = user(9L, "13800138009", "上级用户");
        when(userMapper.selectById(20L)).thenReturn(currentUser);
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("referral:lock:20"), eq("1"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(429, response.getCode());
        assertEquals("操作频繁，请稍后再试", response.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindParentUpdatesCurrentUserWithoutChangingParentStoreCountWhenLockAcquired() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        User parent = user(9L, "13800138009", "上级用户");
        parent.setTreePath("/0/1/");
        parent.setStoreCount(null);
        when(userMapper.selectById(20L)).thenReturn(currentUser);
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("referral:lock:20"), eq("1"), anyLong(), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(200, response.getCode());
        assertEquals(20L, response.getData().get("userId"));
        assertEquals(9L, response.getData().get("parentId"));
        assertEquals("/0/1/9/", response.getData().get("treePath"));
        assertEquals(9L, currentUser.getParentId());
        assertEquals("/0/1/9/", currentUser.getTreePath());
        assertNull(parent.getStoreCount());
        verify(userMapper).updateById(currentUser);
        verify(teamService).evictTeamTreeCaches(currentUser);
        verify(redisTemplate).delete("referral:lock:20");
    }

    @Test
    void bindParentTreatsZeroParentIdOnCurrentUserAsUnbound() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        currentUser.setParentId(0L);
        User parent = user(9L, "13800138009", "上级用户");
        parent.setTreePath("/0/1/");
        parent.setStoreCount(1);
        when(userMapper.selectById(20L)).thenReturn(currentUser);
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("referral:lock:20"), eq("1"), anyLong(), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(200, response.getCode());
        assertEquals(9L, currentUser.getParentId());
        assertEquals("/0/1/9/", currentUser.getTreePath());
        assertEquals(1, parent.getStoreCount());
        verify(userMapper).updateById(currentUser);
        verify(teamService).evictTeamTreeCaches(currentUser);
        verify(redisTemplate).delete("referral:lock:20");
    }

    @Test
    void bindParentUsesDefaultParentTreePathWithoutChangingParentStoreCount() {
        when(request.getAttribute("userId")).thenReturn(20L);
        User currentUser = user(20L, "13800138020", "当前用户");
        User parent = user(9L, "13800138009", "上级用户");
        parent.setTreePath(null);
        parent.setStoreCount(2);
        when(userMapper.selectById(20L)).thenReturn(currentUser);
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("referral:lock:20"), eq("1"), anyLong(), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        ApiResponse<Map<String, Object>> response = controller.bindParent(9L, request);

        assertEquals(200, response.getCode());
        assertEquals("/0/9/", response.getData().get("treePath"));
        assertEquals("/0/9/", currentUser.getTreePath());
        assertEquals(2, parent.getStoreCount());
        verify(userMapper).updateById(currentUser);
        verify(teamService).evictTeamTreeCaches(currentUser);
        verify(redisTemplate).delete("referral:lock:20");
    }

    @Test
    void lockParentRejectsInvalidMobile() {
        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "123",
                "parentId", 9));

        assertEquals(400, response.getCode());
        assertEquals("手机号格式不正确", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void lockParentRejectsMissingMobile() {
        Map<String, Object> body = new HashMap<>();
        body.put("parentId", 9);

        ApiResponse<Map<String, Object>> response = controller.lockParent(body);

        assertEquals(400, response.getCode());
        assertEquals("手机号格式不正确", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void lockParentRejectsInvalidParentId() {
        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", 0));

        assertEquals(400, response.getCode());
        assertEquals("上级ID无效", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void lockParentRejectsUnsupportedParentIdType() {
        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", "abc"));

        assertEquals(400, response.getCode());
        assertEquals("上级ID无效", response.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void lockParentRejectsMissingParent() {
        when(userMapper.selectById(9L)).thenReturn(null);

        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", 9L));

        assertEquals(404, response.getCode());
        assertEquals("上级用户不存在，请检查二维码", response.getMessage());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void lockParentStoresInviteBindingForValidParent() {
        User parent = user(9L, "13800138009", "上级用户");
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", 9));

        assertEquals(200, response.getCode());
        assertEquals("13800138020", response.getData().get("mobile"));
        assertEquals(9L, response.getData().get("parentId"));
        assertEquals("上级用户", response.getData().get("parentNickname"));
        assertTrue((Boolean) response.getData().get("locked"));
        verify(valueOperations).set(
                eq("invite:bind:13800138020"),
                argThat(value -> value instanceof String text
                        && text.contains("\"parentId\":9")
                        && text.contains("\"parentMobile\":\"13800138009\"")),
                eq(30L),
                eq(TimeUnit.MINUTES));
    }

    @Test
    void lockParentAcceptsLongParentId() {
        User parent = user(9L, "13800138009", "上级用户");
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", 9L));

        assertEquals(200, response.getCode());
        assertEquals(9L, response.getData().get("parentId"));
        verify(valueOperations).set(eq("invite:bind:13800138020"), any(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void lockParentAcceptsStringParentId() {
        User parent = user(9L, "13800138009", "上级用户");
        when(userMapper.selectById(9L)).thenReturn(parent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ApiResponse<Map<String, Object>> response = controller.lockParent(Map.of(
                "mobile", "13800138020",
                "parentId", "9"));

        assertEquals(200, response.getCode());
        assertEquals(9L, response.getData().get("parentId"));
        verify(valueOperations).set(eq("invite:bind:13800138020"), any(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getLockedParentReturnsUnlockedWhenNoRedisData() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("invite:bind:13800138020")).thenReturn(null);

        ApiResponse<Map<String, Object>> response = controller.getLockedParent("13800138020");

        assertEquals(200, response.getCode());
        assertFalse((Boolean) response.getData().get("locked"));
    }

    @Test
    void getLockedParentReturnsStoredData() {
        String lockedData = """
                {"parentId":9,"locked":true}
                """;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("invite:bind:13800138020")).thenReturn(lockedData);

        ApiResponse<Map<String, Object>> response = controller.getLockedParent("13800138020");

        assertEquals(200, response.getCode());
        assertEquals(9L, ((Number) response.getData().get("parentId")).longValue());
        assertEquals(true, response.getData().get("locked"));
    }

    private User user(Long id, String mobile, String nickname) {
        User user = new User();
        user.setId(id);
        user.setMobile(mobile);
        user.setNickname(nickname);
        return user;
    }
}
