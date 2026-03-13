package com.gridauthority.coordinator.infrastructure.kms;

import com.gridauthority.coordinator.domain.exceptions.CanonicalSerializationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanonicalJsonMapperTest {

    private final CanonicalJsonMapper mapper = new CanonicalJsonMapper();

    @Test
    void writeCanonical_shouldProduceDeterministicBytes_forSameInput() {
        var ctx = new CommandSigningContext("SHUTDOWN", "device-1", 1_000_000L);

        byte[] first  = mapper.writeCanonical(ctx);
        byte[] second = mapper.writeCanonical(ctx);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void writeCanonicalAsString_shouldSortPropertiesAlphabetically() {
        var ctx = new CommandSigningContext("SHUTDOWN", "device-1", 1_000_000L);

        String json = mapper.writeCanonicalAsString(ctx);

        // alphabetical: action < deviceId < issuedAt
        int actionIdx   = json.indexOf("\"action\"");
        int deviceIdIdx = json.indexOf("\"deviceId\"");
        int issuedAtIdx = json.indexOf("\"issuedAt\"");

        assertThat(actionIdx).isLessThan(deviceIdIdx);
        assertThat(deviceIdIdx).isLessThan(issuedAtIdx);
    }

    @Test
    void writeCanonicalAsString_shouldProduceValidJson() {
        var ctx = new CommandSigningContext("RESTART", "dev-42", 9999L);

        String json = mapper.writeCanonicalAsString(ctx);

        assertThat(json).contains("\"action\":\"RESTART\"");
        assertThat(json).contains("\"deviceId\":\"dev-42\"");
        assertThat(json).contains("\"issuedAt\":9999");
    }

    @Test
    void writeCanonical_shouldSortMapKeysDeterministically() {
        // Map with keys in non-alphabetical order
        Map<String, Object> payload = Map.of("z", 1, "a", 2, "m", 3);

        String json = mapper.writeCanonicalAsString(payload);

        int aIdx = json.indexOf("\"a\"");
        int mIdx = json.indexOf("\"m\"");
        int zIdx = json.indexOf("\"z\"");

        assertThat(aIdx).isLessThan(mIdx);
        assertThat(mIdx).isLessThan(zIdx);
    }

    @Test
    void writeCanonical_shouldThrowCanonicalSerializationException_onUnserializableType() {
        // Jackson calls getters during serialization; a getter that throws forces a JsonProcessingException,
        // which CanonicalJsonMapper must wrap in CanonicalSerializationException.
        Object unserializable = new Object() {
            public String getValue() {
                throw new UnsupportedOperationException("simulated serialization failure");
            }
        };

        assertThatThrownBy(() -> mapper.writeCanonical(unserializable))
                .isInstanceOf(CanonicalSerializationException.class);
    }

    @Test
    void writeCanonical_andWriteCanonicalAsString_shouldBeConsistent() {
        var ctx = new CommandSigningContext("SHUTDOWN", "device-99", 12345L);

        byte[] bytes  = mapper.writeCanonical(ctx);
        String string = mapper.writeCanonicalAsString(ctx);

        assertThat(new String(bytes)).isEqualTo(string);
    }
}
