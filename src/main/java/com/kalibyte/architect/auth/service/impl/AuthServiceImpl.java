package com.kalibyte.architect.auth.service.impl;

import com.kalibyte.architect.auth.dto.*;
import com.kalibyte.architect.auth.entity.Role;
import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.mapper.AuthMapper;
import com.kalibyte.architect.auth.repository.RoleRepository;
import com.kalibyte.architect.auth.repository.UserRepository;
import com.kalibyte.architect.auth.security.token.CustomUserDetails;
import com.kalibyte.architect.auth.security.token.JwtTokenProvider;
import com.kalibyte.architect.auth.service.AuthService;
import com.kalibyte.architect.auth.service.RefreshTokenService;
import com.kalibyte.architect.common.annotation.LoggableAction;
import com.kalibyte.architect.common.exception.AttendanceAlreadyMarkedException;
import com.kalibyte.architect.common.exception.BusinessException;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.common.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final RefreshTokenService refreshTokenService;
    private final com.kalibyte.architect.attendance.service.AttendanceService attendanceService;

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assert userDetails != null;
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Trigger automatic login for attendance, but don't block login if already marked
        try {
            attendanceService.automaticLogin(user);
        } catch (AttendanceAlreadyMarkedException e) {
            // Already logged in today, continue with login
        }

        String jwt = tokenProvider.generateToken(userDetails);
        var refreshToken = refreshTokenService.createRefreshToken(user);

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return LoginResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        var oldRefreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        var newRefreshToken = refreshTokenService.rotateToken(oldRefreshToken);
        User user = newRefreshToken.getUser();

        CustomUserDetails userDetails = CustomUserDetails.create(user);

        String token = tokenProvider.generateToken(userDetails);

        return TokenRefreshResponse.builder()
                .accessToken(token)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    @Override
    public void logout(LogoutRequest request) {
        var oldRefreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        attendanceService.automaticLogout(oldRefreshToken.getUser().getId());
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    @Override
    @LoggableAction("CREATE_USER")
    public UserResponse createUser(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new BusinessException(
                    "Password must be 8-20 characters long and include uppercase, lowercase, number and special character"
            );
        }

        var role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BusinessException("Invalid role"));

        User user = authMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(role));
        
        // Ensure skills is never null
        if (request.getSkills() != null) {
            user.setSkills(request.getSkills());
        } else {
            user.setSkills(new HashSet<>());
        }

        return authMapper.toResponse(userRepository.save(user));
    }


    @Override
    @LoggableAction("CHANGE_PASSWORD")
    public void changePassword(ChangePasswordRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException("User not authenticated");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BusinessException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Prevent same password reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password cannot be same as current password");
        }

        // Validate new password strength
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new BusinessException(
                    "Password must be 8-20 characters long and include uppercase, lowercase, number and special character"
            );
        }


        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user);
    }

    @Override
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserResponse getUserById(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        return authMapper.toResponse(user);
    }

    // Prevent users from deleting their own accounts
    @Override
    @LoggableAction("DELETE_USER")
    public void deleteUser(UUID id) {

        CustomUserDetails currentUser =
                (CustomUserDetails) Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication()).getPrincipal();

        assert currentUser != null;
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("You cannot delete your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        userRepository.delete(user);
    }

    @Override
    @LoggableAction("DISABLE_USER")
    public void disableUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    @LoggableAction("ENABLE_USER")
    public void enableUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setEnabled(true);
        userRepository.save(user);
    }

    // Implement pagination for user listing
    @Override
    @LoggableAction("GET_ALL_USERS")
    public PageResponse<UserResponse> getAllUsers(int page, int size) {

        Page<User> users = userRepository.findAll(PageRequest.of(page, size));

        return PageResponse.from(users, authMapper::toResponse);
    }

    @Override
    public PageResponse<UserResponse> getUsersBySkill(com.kalibyte.architect.auth.entity.enums.SkillType skill, int page, int size) {
        Page<User> users = userRepository.findBySkillsContaining(skill, PageRequest.of(page, size));
        return PageResponse.from(users, authMapper::toResponse);
    }
}

