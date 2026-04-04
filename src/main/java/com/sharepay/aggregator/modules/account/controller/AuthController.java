package com.sharepay.aggregator.modules.account.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification")
@io.swagger.v3.oas.annotations.security.SecurityRequirements()
public class AuthController {
    
}
