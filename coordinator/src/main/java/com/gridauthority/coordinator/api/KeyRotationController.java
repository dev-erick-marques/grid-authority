package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.infrastructure.hcs.AuthorityKeyPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class KeyRotationController {

    private final AuthorityKeyPublisher authorityKeyPublisher;

    @PostMapping("/rotate-key")
    public ResponseEntity<Void> rotateKey() {
       authorityKeyPublisher.publishIfRotated();
        return ResponseEntity.noContent().build();
    }
}
