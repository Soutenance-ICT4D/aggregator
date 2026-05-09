package com.sharepay.aggregator.modules.admin.dto.request;

import com.sharepay.aggregator.shared.constant.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateStaffRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
             message = "Au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial.")
    private String password;

    @NotNull
    private Role role;

    private String phone;
}
