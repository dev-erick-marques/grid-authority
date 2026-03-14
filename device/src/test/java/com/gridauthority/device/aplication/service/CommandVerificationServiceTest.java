package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.exception.SignatureVerificationException;
import com.gridauthority.device.infrastructure.config.SignatureVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommandVerificationServiceTest {

    private static final String CANONICAL_JSON =
            "{\"action\":\"SHUTDOWN\",\"deviceId\":\"device-01\",\"issuedAt\":1000}";

    private KeyPair keyPair;
    private HcsKeyResolver hcsKeyResolver;
    private SignatureVerificationProperties properties;
    private CommandVerificationService service;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(256);
        keyPair = gen.generateKeyPair();

        hcsKeyResolver = mock(HcsKeyResolver.class);
        when(hcsKeyResolver.getResolvedPublicKey()).thenReturn(keyPair.getPublic());

        properties = new SignatureVerificationProperties();
        properties.setEnabled(true);
        properties.setTimestampToleranceMs(30_000);

        service = new CommandVerificationService(hcsKeyResolver, properties);
    }

    @Test
    void verificationDisabledShouldAcceptAnyCommand() {
        properties.setEnabled(false);
        long issuedAt = System.currentTimeMillis();
        assertThatNoException().isThrownBy(
                () -> service.verify("not-a-real-sig", CANONICAL_JSON, issuedAt)
        );
        verifyNoInteractions(hcsKeyResolver);
    }

    @Test
    void validSignatureShouldPass() throws Exception {
        String sig = sign(CANONICAL_JSON, keyPair.getPrivate());
        long issuedAt = System.currentTimeMillis();
        assertThatNoException().isThrownBy(
                () -> service.verify(sig, CANONICAL_JSON, issuedAt)
        );
    }

    @Test
    void tamperedPayloadShouldFail() throws Exception {
        String sig = sign(CANONICAL_JSON, keyPair.getPrivate());
        long issuedAt = System.currentTimeMillis();
        assertThatThrownBy(
                () -> service.verify(sig, "{\"action\":\"RESTART\"}", issuedAt)
        ).isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void wrongKeyShouldFail() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(256);
        KeyPair other = gen.generateKeyPair();

        String sig = sign(CANONICAL_JSON, other.getPrivate());
        long issuedAt = System.currentTimeMillis();
        assertThatThrownBy(
                () -> service.verify(sig, CANONICAL_JSON, issuedAt)
        ).isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void expiredTimestampShouldFail() throws Exception {
        String sig = sign(CANONICAL_JSON, keyPair.getPrivate());
        long expired = System.currentTimeMillis() - 60_000;
        assertThatThrownBy(
                () -> service.verify(sig, CANONICAL_JSON, expired)
        ).isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void futureTimestampShouldFail() throws Exception {
        String sig = sign(CANONICAL_JSON, keyPair.getPrivate());
        long future = System.currentTimeMillis() + 60_000;
        assertThatThrownBy(
                () -> service.verify(sig, CANONICAL_JSON, future)
        ).isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void unresolvedPublicKeyShouldFail() {
        when(hcsKeyResolver.getResolvedPublicKey()).thenReturn(null);
        long issuedAt = System.currentTimeMillis();
        assertThatThrownBy(
                () -> service.verify("any", CANONICAL_JSON, issuedAt)
        ).isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Public key not resolved");
    }

    @Test
    void malformedBase64SignatureShouldFail() {
        long issuedAt = System.currentTimeMillis();
        assertThatThrownBy(
                () -> service.verify("!!!not-base64!!!", CANONICAL_JSON, issuedAt)
        ).isInstanceOf(SignatureVerificationException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String sign(String payload, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(privateKey);
        sig.update(payload.getBytes());
        return Base64.getEncoder().encodeToString(sig.sign());
    }
}