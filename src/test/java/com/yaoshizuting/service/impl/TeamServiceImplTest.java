package com.yaoshizuting.service.impl;

import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.GytUserHierarchyMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class TeamServiceImplTest {

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
    }
}
