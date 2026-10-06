package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.CommandSigningContext;
import com.gridauthority.device.domain.exception.SignatureVerificationException;
import com.gridauthority.device.infrastructure.config.SignatureVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandVerificationServiceTest {

    private KeyPair keyPair;
    private HcsKeyResolver hcsKeyResolver;
    private SignatureVerificationProperties properties;
    private CommandVerificationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(256);
        keyPair = gen.generateKeyPair();

        hcsKeyResolver = mock(HcsKeyResolver.class);
        when(hcsKeyResolver.getResolvedPublicKey()).thenReturn(keyPair.getPublic());

        properties = new SignatureVerificationProperties();
        properties.setTimestampToleranceMs(30_000);

        objectMapper = new ObjectMapper();

        service = new CommandVerificationService(hcsKeyResolver, properties, objectMapper);
        service.init();
    }

    @Test
    void validSignatureShouldPassAndReturnContext() throws Exception {
        String commandId = UUID.randomUUID().toString();
        String canonical = buildCanonical("SHUTDOWN", commandId, "device-01", System.currentTimeMillis());
        String sig = sign(canonical, keyPair.getPrivate());

        CommandSigningContext ctx = service.verify(sig, canonical);

        assertThat(ctx.action()).isEqualTo("SHUTDOWN");
        assertThat(ctx.commandId()).isEqualTo(commandId);
        assertThat(ctx.deviceId()).isEqualTo("device-01");
    }

    @Test
    void tamperedCanonicalShouldFail() throws Exception {
        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01", System.currentTimeMillis());
        String sig = sign(canonical, keyPair.getPrivate());

        String tampered = canonical.replace("SHUTDOWN", "RESTART");

        assertThatThrownBy(() -> service.verify(sig, tampered))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void wrongKeyShouldFail() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(256);
        KeyPair other = gen.generateKeyPair();

        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01", System.currentTimeMillis());
        String sig = sign(canonical, other.getPrivate());

        assertThatThrownBy(() -> service.verify(sig, canonical))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void expiredTimestampShouldFail() throws Exception {
        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01",
                System.currentTimeMillis() - 60_000);
        String sig = sign(canonical, keyPair.getPrivate());

        assertThatThrownBy(() -> service.verify(sig, canonical))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void futureTimestampShouldFail() throws Exception {
        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01",
                System.currentTimeMillis() + 60_000);
        String sig = sign(canonical, keyPair.getPrivate());

        assertThatThrownBy(() -> service.verify(sig, canonical))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void unresolvedPublicKeyShouldFail() throws Exception {
        when(hcsKeyResolver.getResolvedPublicKey()).thenReturn(null);
        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01", System.currentTimeMillis());
        String sig = sign(canonical, keyPair.getPrivate());

        assertThatThrownBy(() -> service.verify(sig, canonical))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Public key not resolved");
    }

    @Test
    void malformedBase64SignatureShouldFail() throws Exception {
        String canonical = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), "device-01", System.currentTimeMillis());

        assertThatThrownBy(() -> service.verify("!!!not-base64!!!", canonical))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void replayWithinWindowShouldFail() throws Exception {
        String commandId = UUID.randomUUID().toString();
        String canonical = buildCanonical("SHUTDOWN", commandId, "device-01", System.currentTimeMillis());
        String sig = sign(canonical, keyPair.getPrivate());

        service.verify(sig, canonical);

        assertThatThrownBy(() -> service.verify(sig, canonical))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("duplicate commandId");
    }

    @Test
    void differentCommandIdsShouldBothPass() throws Exception {
        long issuedAt = System.currentTimeMillis();
        String deviceId = "device-01";

        String canonical1 = buildCanonical("SHUTDOWN", UUID.randomUUID().toString(), deviceId, issuedAt);
        String sig1 = sign(canonical1, keyPair.getPrivate());

        String canonical2 = buildCanonical("RESTART", UUID.randomUUID().toString(), deviceId, issuedAt);
        String sig2 = sign(canonical2, keyPair.getPrivate());

        CommandSigningContext ctx1 = service.verify(sig1, canonical1);
        CommandSigningContext ctx2 = service.verify(sig2, canonical2);

        assertThat(ctx1.action()).isEqualTo("SHUTDOWN");
        assertThat(ctx2.action()).isEqualTo("RESTART");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String buildCanonical(String action, String commandId, String deviceId, long issuedAt) {
        // matches CanonicalJsonMapper alphabetical ordering: action < commandId < deviceId < issuedAt
        return String.format(
                "{\"action\":\"%s\",\"commandId\":\"%s\",\"deviceId\":\"%s\",\"issuedAt\":%d}",
                action, commandId, deviceId, issuedAt);
    }

    private String sign(String payload, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(privateKey);
        sig.update(payload.getBytes());
        return Base64.getEncoder().encodeToString(sig.sign());
    }
}