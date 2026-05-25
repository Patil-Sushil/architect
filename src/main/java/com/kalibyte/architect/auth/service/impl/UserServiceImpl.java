package com.kalibyte.architect.auth.service.impl;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.repository.UserRepository;
import com.kalibyte.architect.auth.service.UserService;
import com.kalibyte.architect.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
    }
}
