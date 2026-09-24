package com.yeyamo_mobile.api.feed_service.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class FeedProjectionLocationMigrationTest {

    @Test
    void addsEveryLocationColumnReadByFeedPostEntity() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:feed_projection_location;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE feed_posts (post_id UUID PRIMARY KEY)");
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V8__add_feed_projection_location_columns.sql"));

            assertColumnExists(connection, "CITY_ID");
            assertColumnExists(connection, "LATITUDE");
            assertColumnExists(connection, "LONGITUDE");

            try (Statement statement = connection.createStatement()) {
                assertThrows(SQLException.class,
                        () -> statement.executeUpdate("INSERT INTO feed_posts (post_id, latitude) VALUES (RANDOM_UUID(), 4.05)"));
            }
        }
    }

    private void assertColumnExists(Connection connection, String column) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "FEED_POSTS", column)) {
            assertTrue(columns.next(), () -> "Missing feed_posts." + column.toLowerCase());
        }
    }
}
