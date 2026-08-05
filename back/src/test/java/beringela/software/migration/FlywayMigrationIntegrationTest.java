package beringela.software.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pratten")
            .withUsername("pratten")
            .withPassword("pratten");

    @Test
    void appliesAllMigrationsOnPostgres() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();
        assertTrue(result.migrationsExecuted >= 9);

        try (Connection connection = postgres.createConnection("");
                ResultSet versions = connection.createStatement().executeQuery(
                        "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1");
                ResultSet tables = connection.createStatement().executeQuery(
                        "SELECT to_regclass('public.menu_item_ingredients')")) {
            assertTrue(versions.next());
            assertEquals("9", versions.getString("version"));
            assertTrue(tables.next());
            assertEquals("menu_item_ingredients", tables.getString(1));
        }
    }
}
