package com.gridauthority.coordinator.infrastructure.hcs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadHasherTest {

    @Test
    void sha256_shouldReturnKnownHash_forKnownInput() {
        // echo -n "hello" | sha256sum → 2cf24dba...
        String hash = PayloadHasher.sha256("hello");
        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void sha256_shouldReturnDifferentHashes_forDifferentInputs() {
        String hash1 = PayloadHasher.sha256("SHUTDOWN:device-1:1000");
        String hash2 = PayloadHasher.sha256("SHUTDOWN:device-1:1001");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void sha256_shouldReturnSameHash_forSameInput() {
        String input = "{\"action\":\"SHUTDOWN\",\"deviceId\":\"dev-1\",\"issuedAt\":12345}";

        assertThat(PayloadHasher.sha256(input)).isEqualTo(PayloadHasher.sha256(input));
    }

    @Test
    void sha256_shouldReturn64CharHexString() {
        String hash = PayloadHasher.sha256("any payload");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void sha256_shouldHandleEmptyString() {
        // SHA-256 of "" is well-defined
        String hash = PayloadHasher.sha256("");
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void sha256_shouldHandleUnicodeInput() {
        String hash = PayloadHasher.sha256("voltagem: 230V ± 10%");
        assertThat(hash).hasSize(64);
    }
}
