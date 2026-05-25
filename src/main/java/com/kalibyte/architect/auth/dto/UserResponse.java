package com.kalibyte.architect.auth.dto;

import com.kalibyte.architect.auth.entity.enums.SkillType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String bankAccountNumber;
    private String bankIfsc;
    private LocalDate dateOfJoining;
    private boolean enabled;
    private List<String> roles;
    private Set<SkillType> skills;
}

