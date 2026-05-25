package com.kalibyte.architect.auth.service;

import com.kalibyte.architect.auth.dto.*;
import com.kalibyte.architect.auth.entity.Role;
import com.kalibyte.architect.auth.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    TokenRefreshResponse refreshToken(TokenRefreshRequest request);

    void logout(LogoutRequest request);

    UserResponse createUser(UserRegistrationRequest request);

    void changePassword(ChangePasswordRequest request);

    List<Role> getRoles();

    List<User> getAllUsers();

    UserResponse getUserById(UUID id);

    void deleteUser(UUID id);

    void disableUser(UUID id);

    void enableUser(UUID id);

    com.kalibyte.architect.common.response.PageResponse<UserResponse> getAllUsers(int page, int size);

    com.kalibyte.architect.common.response.PageResponse<UserResponse> getUsersBySkill(com.kalibyte.architect.auth.entity.enums.SkillType skill, int page, int size);
}
