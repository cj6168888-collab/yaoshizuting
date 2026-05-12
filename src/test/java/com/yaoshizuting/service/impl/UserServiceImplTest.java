package com.yaoshizuting.service.impl;

import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, jwtUtils, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loginRejectsExpiredSmsCode() {
        LoginRequest request = loginRequest("13800138000", "123456", null);
        when(valueOperations.get("sms:code:13800138000")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.login(request));

        assertEquals("验证码已过期，请重新获取", exception.getMessage());
        verify(userMapper, never()).selectByMobile(any());
    }

    @Test
    void loginRejectsWrongSmsCode() {
        LoginRequest request = loginRequest("13800138000", "000000", null);
        when(valueOperations.get("sms:code:13800138000")).thenReturn("123456");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.login(request));

        assertEquals("验证码错误", exception.getMessage());
        verify(userMapper, never()).selectByMobile(any());
    }

    @Test
    void loginReturnsExistingUserAndDeletesSmsCode() {
        LoginRequest request = loginRequest("13800138000", "123456 ", null);
        User user = user(8L, "13800138000", 1);
        user.setNickname("老用户");
        user.setAvatar("/avatar.png");
        user.setParentId(2L);
        user.setTreePath("/0/2/");
        when(valueOperations.get("sms:code:13800138000")).thenReturn(" 123456 ");
        when(valueOperations.get("invite:bind:13800138000")).thenReturn(null);
        when(userMapper.selectByMobile("13800138000")).thenReturn(user);
        when(jwtUtils.generateToken(8L, "13800138000", 1)).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(8L, response.getUserId());
        assertEquals(1, response.getRole());
        assertEquals("老用户", response.getNickname());
        assertEquals("/avatar.png", response.getAvatar());
        assertEquals(2L, response.getParentId());
        assertEquals("/0/2/", response.getTreePath());
        verify(redisTemplate).delete("sms:code:13800138000");
    }

    @Test
    void loginBindsExistingUnboundUserFromLockedInvite() {
        LoginRequest request = loginRequest("13800138001", "123456", null);
        User user = user(8L, "13800138001", 0);
        user.setParentId(0L);
        User parent = user(2L, "13800138002", 2);
        parent.setTreePath("/0/");
        when(valueOperations.get("sms:code:13800138001")).thenReturn("123456");
        when(valueOperations.get("invite:bind:13800138001")).thenReturn(Map.of("parentId", 2L));
        when(userMapper.selectByMobile("13800138001")).thenReturn(user);
        when(userMapper.selectById(2L)).thenReturn(parent);
        when(jwtUtils.generateToken(8L, "13800138001", 0)).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(2L, user.getParentId());
        assertEquals("/0/2/", user.getTreePath());
        verify(userMapper).updateById(user);
        verify(redisTemplate).delete("invite:bind:13800138001");
        verify(redisTemplate).delete("sms:code:13800138001");
    }

    @Test
    void loginRejectsFrozenUserAfterInviteCleanup() {
        LoginRequest request = loginRequest("13800138000", "123456", null);
        User user = user(8L, "13800138000", 0);
        user.setStatus(0);
        when(valueOperations.get("sms:code:13800138000")).thenReturn("123456");
        when(valueOperations.get("invite:bind:13800138000")).thenReturn(Map.of("parentId", 2L));
        when(userMapper.selectByMobile("13800138000")).thenReturn(user);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.login(request));

        assertEquals("账号已被冻结，请联系管理员", exception.getMessage());
        verify(redisTemplate).delete("invite:bind:13800138000");
        verify(redisTemplate, never()).delete("sms:code:13800138000");
    }

    @Test
    void createNewUserUsesInviteParentAndIncrementsStoreCount() {
        User parent = user(2L, "13800138002", 2);
        parent.setTreePath("/0/");
        parent.setStoreCount(5);
        when(userMapper.selectByMobile("13800138002")).thenReturn(parent);
        when(userMapper.selectById(2L)).thenReturn(parent);
        doAnswer(invocation -> {
            User inserted = invocation.getArgument(0);
            inserted.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        User created = userService.createNewUser("13800138003", "13800138002");

        assertEquals(10L, created.getId());
        assertEquals("13800138003", created.getMobile());
        assertEquals("用户8003", created.getNickname());
        assertEquals(0, created.getRole());
        assertEquals(2L, created.getParentId());
        assertEquals("/0/2/", created.getTreePath());
        assertEquals(BigDecimal.ZERO, created.getBalance());
        assertEquals(BigDecimal.ZERO, created.getTotalEarnings());
        assertEquals(6, parent.getStoreCount());
        verify(userMapper).insert(created);
        verify(userMapper).updateById(parent);
    }

    @Test
    void getUserByIdDelegatesToMapper() {
        User user = user(3L, "13800138003", 0);
        when(userMapper.selectById(3L)).thenReturn(user);

        assertEquals(user, userService.getUserById(3L));
    }

    @Test
    void getUserByMobileDelegatesToMapper() {
        User user = user(3L, "13800138003", 0);
        when(userMapper.selectByMobile("13800138003")).thenReturn(user);

        assertEquals(user, userService.getUserByMobile("13800138003"));
    }

    @Test
    void hasStoreReturnsTrueOnlyForStoreOrAbove() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "13800138001", 1));
        when(userMapper.selectById(2L)).thenReturn(user(2L, "13800138002", 0));
        when(userMapper.selectById(3L)).thenReturn(null);

        assertTrue(userService.hasStore(1L));
        assertFalse(userService.hasStore(2L));
        assertFalse(userService.hasStore(3L));
    }

    @Test
    void buildTreePathReturnsRootForMissingParent() {
        when(userMapper.selectById(9L)).thenReturn(null);

        assertEquals("/0/", userService.buildTreePath(null));
        assertEquals("/0/", userService.buildTreePath(0L));
        assertEquals("/0/", userService.buildTreePath(9L));
    }

    @Test
    void buildTreePathAppendsParentId() {
        User parent = user(9L, "13800138009", 2);
        parent.setTreePath("/0/2/");
        when(userMapper.selectById(9L)).thenReturn(parent);

        assertEquals("/0/2/9/", userService.buildTreePath(9L));
    }

    @Test
    void loginCreatesNewUserFromLockedInviteWhenParentExists() {
        LoginRequest request = loginRequest("13800138005", "123456", null);
        User parent = user(2L, "13800138002", 2);
        parent.setTreePath("/0/");
        parent.setStoreCount(0);
        when(valueOperations.get("sms:code:13800138005")).thenReturn("123456");
        when(valueOperations.get("invite:bind:13800138005")).thenReturn(Map.of("parentId", 2L));
        when(userMapper.selectByMobile("13800138005")).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(parent);
        when(userMapper.selectByMobile("13800138002")).thenReturn(parent);
        doAnswer(invocation -> {
            User inserted = invocation.getArgument(0);
            inserted.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(jwtUtils.generateToken(10L, "13800138005", 0)).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(10L, response.getUserId());
        assertEquals(2L, response.getParentId());
        assertEquals("/0/2/", response.getTreePath());
        verify(redisTemplate).delete("invite:bind:13800138005");
        verify(redisTemplate).delete("sms:code:13800138005");
    }

    private LoginRequest loginRequest(String mobile, String code, String inviteCode) {
        LoginRequest request = new LoginRequest();
        request.setMobile(mobile);
        request.setCode(code);
        request.setInviteCode(inviteCode);
        return request;
    }

    private User user(Long id, String mobile, Integer role) {
        User user = new User();
        user.setId(id);
        user.setMobile(mobile);
        user.setRole(role);
        user.setNickname("用户" + mobile.substring(7));
        user.setParentId(0L);
        user.setTreePath("/0/");
        user.setAgentCount(0);
        user.setStoreCount(0);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalEarnings(BigDecimal.ZERO);
        user.setStatus(1);
        return user;
    }
}
