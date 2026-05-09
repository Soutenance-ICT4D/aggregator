package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StaffResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AccountStatus status;
    private OffsetDateTime createdAt;
}
