package com.yaoshizuting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.dto.TeamNodeDTO;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.TeamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEAM_CACHE_PREFIX = "team:tree:";

    @Override
    public List<TeamNodeDTO> getTeamTree(Long userId) {
        String cacheKey = TEAM_CACHE_PREFIX + userId;
        
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TeamNodeDTO>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached team tree: {}", e.getMessage());
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        
        String path = user.getTreePath() != null ? user.getTreePath() : "/0/";
        String descendantPath = path + user.getId() + "/";
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(User::getTreePath, descendantPath)
               .ne(User::getId, userId)
               .eq(User::getDeleted, 0);
        
        List<User> users = userMapper.selectList(wrapper);
        List<TeamNodeDTO> result = new ArrayList<>();
        
        for (User u : users) {
            TeamNodeDTO dto = new TeamNodeDTO();
            dto.setUserId(u.getId());
            dto.setRole(u.getRole());
            dto.setParentId(u.getParentId());
            dto.setTreePath(u.getTreePath());
            dto.setNickname(u.getNickname());
            dto.setMobile(u.getMobile());
            result.add(dto);
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache team tree: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public void evictTeamTreeCaches(User user) {
        if (user == null) {
            return;
        }

        Set<Long> userIds = new LinkedHashSet<>();
        addPositiveId(userIds, user.getId());
        addPositiveId(userIds, user.getParentId());
        if (user.getTreePath() != null) {
            for (String part : user.getTreePath().split("/")) {
                try {
                    addPositiveId(userIds, Long.parseLong(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (Long userId : userIds) {
            try {
                redisTemplate.delete(TEAM_CACHE_PREFIX + userId);
            } catch (Exception e) {
                log.warn("Failed to evict team tree cache: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    private void addPositiveId(Set<Long> userIds, Long userId) {
        if (userId != null && userId > 0) {
            userIds.add(userId);
        }
    }
}
