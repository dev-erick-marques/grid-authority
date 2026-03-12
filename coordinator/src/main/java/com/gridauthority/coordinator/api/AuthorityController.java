package com.gridauthority.coordinator.api;


import com.gridauthority.coordinator.application.dto.PublicKeyResponseDTO;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/authority")
public class AuthorityController {

    private final KmsSigningService kmsSigningService;

    @GetMapping(value = "/public-key")
    public PublicKeyResponseDTO getPublicKey() {
        return kmsSigningService.getPublicKeyResponse();
    }
}