package com.yaoshizuting.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.TeamService;
import com.yaoshizuting.service.UserService;
import com.yaoshizuting.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TeamService teamService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE = 300;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        String mobile = request.getMobile();
        String code = request.getCode();

        String cacheKey = SMS_CODE_PREFIX + mobile;
        Object cachedCodeObj = redisTemplate.opsForValue().get(cacheKey);
        String cachedCode = cachedCodeObj != null ? cachedCodeObj.toString().trim() : null;
        
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        
        log.info("验证码比对: cached={}, input={}", cachedCode, code);
        if (!cachedCode.equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }

        User user = userMapper.selectByMobile(mobile);

        String bindKey = "invite:bind:" + mobile;
        Object bindData = redisTemplate.opsForValue().get(bindKey);
        Map<String, Object> lockedBindData = parseBindData(bindData);

        if (user == null) {
            String inviteCode = request.getInviteCode();
            if (inviteCode == null && lockedBindData != null) {
                Object lockedParentId = lockedBindData.get("parentId");
                if (lockedParentId != null) {
                    User lockedParent = userMapper.selectById(parseLong(lockedParentId));
                    if (lockedParent != null) {
                        inviteCode = lockedParent.getMobile();
                        log.info("从Redis锁定关系获取上级: mobile={}, parentMobile={}", mobile, inviteCode);
                    }
                }
            }
            user = createNewUser(mobile, inviteCode);
        } else if (lockedBindData != null && user.getParentId() == 0) {
            Object lockedParentId = lockedBindData.get("parentId");
            if (lockedParentId != null) {
                Long parentId = parseLong(lockedParentId);
                if (parentId > 0 && !parentId.equals(user.getId())) {
                    user.setParentId(parentId);
                    User lockedParent = userMapper.selectById(parentId);
                    if (lockedParent != null) {
                        String parentPath = lockedParent.getTreePath() != null ? lockedParent.getTreePath() : "/0/";
                        user.setTreePath(parentPath + parentId + "/");
                        userMapper.updateById(user);
                        teamService.evictTeamTreeCaches(user);
                        log.info("扫码绑定上级: userId={}, parentId={}", user.getId(), parentId);
                    }
                }
            }
        }

        if (bindData != null) {
            redisTemplate.delete(bindKey);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被冻结，请联系管理员");
        }

        redisTemplate.delete(cacheKey);

        String token = jwtUtils.generateToken(user.getId(), user.getMobile(), user.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setRole(user.getRole());
        response.setNickname(user.getNickname());
        response.setMobile(user.getMobile());
        response.setAvatar(user.getAvatar());
        response.setParentId(user.getParentId());
        response.setTreePath(user.getTreePath());

        log.info("用户登录成功: mobile={}, role={}", mobile, user.getRole());
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public User createNewUser(String mobile, String inviteCode) {
        Long parentId = 0L;
        String treePath = "/0/";

        if (StrUtil.isNotBlank(inviteCode)) {
            User parent = userMapper.selectByMobile(inviteCode);
            if (parent != null) {
                parentId = parent.getId();
                String parentPath = parent.getTreePath() != null ? parent.getTreePath() : "/0/";
                treePath = parentPath + parent.getId() + "/";
            }
        }

        User user = new User();
        user.setMobile(mobile);
        user.setNickname("用户" + mobile.substring(7));
        user.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        user.setRole(UserRole.MEMBER.getCode());
        user.setParentId(parentId);
        user.setTreePath(treePath);
        user.setAgentCount(0);
        user.setStoreCount(0);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalEarnings(BigDecimal.ZERO);
        user.setStatus(1);

        userMapper.insert(user);
        teamService.evictTeamTreeCaches(user);

        log.info("创建新用户: mobile={}, parentId={}", mobile, parentId);
        return user;
    }

    private Map<String, Object> parseBindData(Object bindData) {
        if (bindData instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (bindData instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析锁定上级关系失败: {}", e.getMessage());
            }
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    public boolean hasStore(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() >= UserRole.STORE.getCode();
    }

    @Override
    public String buildTreePath(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "/0/";
        }
        User parent = userMapper.selectById(parentId);
        if (parent == null) {
            return "/0/";
        }
        return parent.getTreePath() + parent.getId() + "/";
    }
}
