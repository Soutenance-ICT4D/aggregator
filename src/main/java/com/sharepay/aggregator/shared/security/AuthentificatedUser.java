package com.sharepay.aggregator.shared.security;

import com.sharepay.aggregator.shared.constant.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthentificatedUser {
    private UUID accountId;
    private String email;
    private Role role;
}
