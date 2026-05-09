package com.sharepay.aggregator.modules.admin.dto.request;

import com.sharepay.aggregator.shared.constant.PaymentProviderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertProviderRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private PaymentProviderType type;

    @NotBlank
    @Size(min = 2, max = 3)
    private String country;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin("0.00")
    private BigDecimal feePercentage;

    @Min(0)
    private Long feeFixed;

    @Min(1)
    private Long minAmount;

    @Min(1)
    private Long maxAmount;
}
