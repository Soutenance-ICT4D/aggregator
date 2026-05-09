package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.request.CreateStaffRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.StaffResponse;
import com.sharepay.aggregator.modules.admin.service.AdminStaffService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff")
@Tag(name = "Admin — Staff", description = "Gestion des comptes admin et support")
@SecurityRequirement(name = "bearerAuth")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte admin ou support")
    public ApiResponse<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.success(adminStaffService.createStaff(request));
    }

    @GetMapping
    @Operation(summary = "Lister les membres du staff (admins + support)")
    public ApiResponse<PaginationResponse<StaffResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminStaffService.listStaff(page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activer ou suspendre un membre du staff")
    public ApiResponse<StaffResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(adminStaffService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer (soft delete) un membre du staff")
    public void delete(@PathVariable UUID id) {
        adminStaffService.deleteStaff(id);
    }
}
