package com.sharepay.aggregator.modules.payment.service;

import com.sharepay.aggregator.modules.payment.dto.request.ChargeRequest;
import com.sharepay.aggregator.modules.payment.dto.request.CheckoutRequest;
import com.sharepay.aggregator.modules.payment.dto.response.ChargeResponse;
import com.sharepay.aggregator.modules.payment.dto.response.CheckoutResponse;
import com.sharepay.aggregator.modules.payment.dto.response.PayInStatusResponse;

import java.util.UUID;

public interface PayInService {
    CheckoutResponse createCheckout(UUID apiKeyId, CheckoutRequest request);
    ChargeResponse createCharge(UUID apiKeyId, ChargeRequest request);
    PayInStatusResponse checkStatus(UUID apiKeyId, String reference);
}
