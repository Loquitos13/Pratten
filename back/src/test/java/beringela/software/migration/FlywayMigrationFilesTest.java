package beringela.software.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FlywayMigrationFilesTest {

    @Test
    void migrationsAreSequentialThroughV6() throws Exception {
        Path dir = Path.of("src/main/resources/db/migration");
        for (int version = 1; version <= 9; version++) {
            try (var stream = Files.list(dir)) {
                String prefix = "V" + version + "__";
                boolean found = stream.anyMatch(path -> path.getFileName().toString().startsWith(prefix));
                assertTrue(found, "Missing Flyway migration " + prefix);
            }
        }
    }
}
