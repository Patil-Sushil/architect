package com.kalibyte.architect.auth.dto;

import com.kalibyte.architect.auth.entity.enums.RoleName;
import com.kalibyte.architect.auth.entity.enums.SkillType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace.")
    private String password;

    private RoleName role;

    @NotBlank
    private String name;
    @NotBlank
    private String phone;
    @NotBlank
    private String address;

    private String bankAccountNumber;

    private String bankIfsc;

    @PastOrPresent
    private LocalDate dateOfJoining;

    private Set<SkillType> skills;
}
