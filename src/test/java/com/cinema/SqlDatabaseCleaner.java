package com.cinema;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@RequiredArgsConstructor
@Component
public class SqlDatabaseCleaner {
    private static final String TABLES_NAMES_SQL =
            "SELECT table_name " +
                    "FROM information_schema.tables " +
                    "WHERE table_schema='public'";
    private static final String TRUNCATE_SQL = "TRUNCATE TABLE ";
    private static final String RESTART_IDENTITY = " RESTART IDENTITY CASCADE";

    private final DataSource dataSource;

    public void clean() {
        try (
                Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection
                        .createStatement()
                        .executeQuery(TABLES_NAMES_SQL)
        ) {
            resetTables(connection, resultSet);
        } catch (SQLException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private void resetTables(Connection connection, ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            String tableName = resultSet.getString("table_name");
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(TRUNCATE_SQL + tableName + RESTART_IDENTITY);
            }
        }
    }
}
