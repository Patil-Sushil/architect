package com.kalibyte.architect.auth.mapper;

import com.kalibyte.architect.auth.dto.UserRegistrationRequest;
import com.kalibyte.architect.auth.dto.UserResponse;
import com.kalibyte.architect.auth.entity.Role;
import com.kalibyte.architect.auth.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "skills", source = "skills")
    User toEntity(UserRegistrationRequest request);

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    @Mapping(target = "skills", source = "skills")
    UserResponse toResponse(User user);

    default List<String> mapRoles(java.util.Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
    }
}
