package com.sharepay.aggregator.modules.admin.dto.request;

import com.sharepay.aggregator.shared.constant.KycLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMerchantKycRequest {

    @NotNull
    private KycLevel kycLevel;
}
