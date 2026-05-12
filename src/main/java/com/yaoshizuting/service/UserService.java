package com.yaoshizuting.service;

import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.entity.User;

public interface UserService {

    LoginResponse login(LoginRequest request);

    User getUserById(Long userId);

    User getUserByMobile(String mobile);

    boolean hasStore(Long userId);

    String buildTreePath(Long parentId);
}
