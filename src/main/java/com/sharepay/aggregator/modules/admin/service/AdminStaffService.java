package com.sharepay.aggregator.modules.admin.service;

import com.sharepay.aggregator.modules.admin.dto.request.CreateStaffRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.StaffResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;

import java.util.UUID;

public interface AdminStaffService {
    StaffResponse createStaff(CreateStaffRequest request);
    PaginationResponse<StaffResponse> listStaff(int page, int size);
    StaffResponse updateStatus(UUID staffId, UpdateStatusRequest request);
    void deleteStaff(UUID staffId);
}
