package com.kalibyte.architect.auth.service;

import com.kalibyte.architect.auth.entity.User;

public interface UserService {

    User getByEmail(String email);
}
