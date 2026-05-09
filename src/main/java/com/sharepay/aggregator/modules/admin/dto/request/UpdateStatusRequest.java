package com.sharepay.aggregator.modules.admin.dto.request;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull
    private AccountStatus status;
}
