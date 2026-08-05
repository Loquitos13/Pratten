package beringela.software.common;

import static org.assertj.core.api.Assertions.assertThat;

import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrattenJsonTest {

    @Test
    void serializesInstantFieldsInRecords() {
        Instant at = Instant.parse("2026-08-04T01:00:00Z");
        var payload = new PlatformNotificationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INFO",
                "REMOTE_SESSION_STARTED",
                "Sessão remota",
                "Admin entrou",
                false,
                at);

        String json = PrattenJson.write(payload);
        PlatformNotificationResponse roundTrip =
                PrattenJson.read(json, PlatformNotificationResponse.class);

        assertThat(roundTrip.at()).isEqualTo(at);
        assertThat(json).contains("2026-08-04T01:00:00Z");
    }
}
