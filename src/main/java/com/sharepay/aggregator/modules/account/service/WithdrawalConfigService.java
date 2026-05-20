package com.sharepay.aggregator.modules.account.service;

import com.sharepay.aggregator.modules.account.dto.request.CreateWithdrawalAccountRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateWithdrawalConfigRequest;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalAccountResponse;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalConfigResponse;

import java.util.List;
import java.util.UUID;

public interface WithdrawalConfigService {

    // ── Comptes ───────────────────────────────────────────────────────────────

    List<WithdrawalAccountResponse> getAccounts(UUID userId);

    WithdrawalAccountResponse addAccount(UUID userId, CreateWithdrawalAccountRequest request);

    void deleteAccount(UUID userId, UUID accountId);

    WithdrawalAccountResponse setDefaultAccount(UUID userId, UUID accountId);

    // ── Configuration ─────────────────────────────────────────────────────────

    WithdrawalConfigResponse getConfig(UUID userId);

    WithdrawalConfigResponse updateConfig(UUID userId, UpdateWithdrawalConfigRequest request);
}
