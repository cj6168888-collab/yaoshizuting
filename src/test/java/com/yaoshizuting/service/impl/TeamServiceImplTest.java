package com.yaoshizuting.service.impl;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TeamServiceImpl teamService;

    @Test
    void testGetTeamTree_basicTree() {
        Long userId = 1L;
        User root = new User();
        root.setId(userId);
        root.setTreePath("/0/1/");

        User a = new User();
        a.setId(2L);
        a.setParentId(1L);
        a.setTreePath("/0/1/");
        a.setRole(1);

        User b = new User();
        b.setId(3L);
        b.setParentId(2L);
        b.setTreePath("/0/1/2/");
        b.setRole(2);

        List<User> list = new ArrayList<>();
        list.add(a);
        list.add(b);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(root);
        when(userMapper.selectList(any())).thenReturn(list);

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(2, result.size());
        TeamNodeDTO n1 = result.get(0);
        TeamNodeDTO n2 = result.get(1);
        assertEquals(2L, n1.getUserId().longValue());
        assertEquals(1, n1.getRole().intValue());
        assertEquals(3L, n2.getUserId().longValue());
        assertEquals(2, n2.getRole().intValue());
        verify(valueOperations).set(eq("team:tree:" + userId), any(String.class), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getTeamTreeReturnsCachedTreeWithoutDatabaseLookup() {
        Long userId = 1L;
        String cachedTree = """
                [
                  {
                    "userId": 2,
                    "role": 1,
                    "parentId": 1,
                    "treePath": "/0/1/",
                    "nickname": "缓存会员",
                    "mobile": "13800138002"
                  }
                ]
                """;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn(cachedTree);

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(1, result.size());
        TeamNodeDTO node = result.get(0);
        assertEquals(2L, node.getUserId());
        assertEquals(1, node.getRole());
        assertEquals(1L, node.getParentId());
        assertEquals("/0/1/", node.getTreePath());
        assertEquals("缓存会员", node.getNickname());
        assertEquals("13800138002", node.getMobile());
        verify(userMapper, never()).selectById(any());
        verify(userMapper, never()).selectList(any());
    }

    @Test
    void getTeamTreeFallsBackToDatabaseWhenCachedTreeIsInvalid() {
        Long userId = 1L;
        User root = new User();
        root.setId(userId);
        root.setTreePath("/0/1/");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn("{invalid-json");
        when(userMapper.selectById(userId)).thenReturn(root);
        when(userMapper.selectList(any())).thenReturn(List.of());

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(0, result.size());
        verify(userMapper).selectById(userId);
        verify(userMapper).selectList(any());
    }

    @Test
    void getTeamTreeReturnsEmptyListWhenUserDoesNotExist() {
        Long userId = 404L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(null);

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(0, result.size());
        verify(userMapper, never()).selectList(any());
        verify(valueOperations, never()).set(any(), any(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getTeamTreeUsesDefaultRootPathWhenUserTreePathIsMissing() {
        Long userId = 1L;
        User root = new User();
        root.setId(userId);
        root.setTreePath(null);

        User child = new User();
        child.setId(2L);
        child.setParentId(1L);
        child.setTreePath("/0/");
        child.setRole(1);
        child.setNickname("默认路径会员");
        child.setMobile("13800138002");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(root);
        when(userMapper.selectList(any())).thenReturn(List.of(child));

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(1, result.size());
        TeamNodeDTO node = result.get(0);
        assertEquals(2L, node.getUserId());
        assertEquals(1L, node.getParentId());
        assertEquals("/0/", node.getTreePath());
        assertEquals("默认路径会员", node.getNickname());
        assertEquals("13800138002", node.getMobile());
    }

    @Test
    void getTeamTreeStillReturnsResultWhenCacheSerializationFails() throws Exception {
        Long userId = 1L;
        User root = new User();
        root.setId(userId);
        root.setTreePath("/0/1/");

        User child = new User();
        child.setId(2L);
        child.setParentId(1L);
        child.setTreePath("/0/1/");
        child.setRole(1);
        child.setNickname("缓存失败会员");
        child.setMobile("13800138002");

        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(JsonMappingException.fromUnexpectedIOE(new IOException("serialization failed")));
        ReflectionTestUtils.setField(teamService, "objectMapper", objectMapper);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("team:tree:" + userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(root);
        when(userMapper.selectList(any())).thenReturn(List.of(child));

        List<TeamNodeDTO> result = teamService.getTeamTree(userId);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getUserId());
        verify(valueOperations, never()).set(any(), any(), eq(5L), eq(TimeUnit.MINUTES));
    }
}
