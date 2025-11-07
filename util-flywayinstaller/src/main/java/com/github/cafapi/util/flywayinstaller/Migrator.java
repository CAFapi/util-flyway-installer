/*
 * Copyright 2021-2025 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafapi.util.flywayinstaller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private static final String CREATE_DATABASE_BASE = "CREATE DATABASE %I";
    private static final String DOES_DATABASE_EXIST
        = "SELECT EXISTS (SELECT NULL FROM pg_catalog.pg_database WHERE lower( datname ) = lower( ? ));";
    private static final String WITH_COLLATION = "WITH TEMPLATE template0 LC_COLLATE = %I LC_CTYPE = %I";

    public enum Collation {

        C("C"),
        UTF_8("en_US.UTF-8");

        private final String value;
        Collation(final String value)
        {
            this.value = value;
        }

        public String value() {
            return value;
        }

    }

    private Migrator()
    {
    }

    @Deprecated
    public static void migrate(
        final String dbHost,
        final int dbPort,
        final String dbName,
        final String username,
        final List<String> secretKeys,
        final String password
    ) throws SQLException
    {
        migrate(dbHost, dbPort, dbName, username, secretKeys, password, null, null);
    }

    public static void migrate(
        final String dbHost,
        final int dbPort,
        final String dbName,
        final String username,
        final List<String> secretKeys,
        final String password,
        final String schemaName,
        final Collation collation
    ) throws SQLException
    {
        logReceivedArgumentsIfDebug(dbHost, dbPort, dbName, username, secretKeys, schemaName, collation);

        LOGGER.info("Checking connection ...");

        final PGSimpleDataSource dbSource = new PGSimpleDataSource();
        dbSource.setServerNames(new String[]{dbHost});
        dbSource.setPortNumbers(new int[]{dbPort});
        dbSource.setUser(username);
        dbSource.setPassword(password);

        try (final Connection connection = dbSource.getConnection()) {
            if (!doesDbExist(connection, dbName)) {
                LOGGER.debug("reset or createDB");
                createDatabase(connection, dbName, collation);
            }
        }
        LOGGER.info("Connection Ok. Starting migration ...");

        // Once we made sure that the database exists, we add the information in the dataSource
        dbSource.setDatabaseName(dbName);

        // Set default schema to user-specified schema if provided, otherwise use "public"
        final String defaultSchema = schemaName == null ? "public" : schemaName;
        LOGGER.info("Using schema: {}", defaultSchema);

        final Flyway flyway = Flyway.configure()
            .dataSource(dbSource)
            .baselineOnMigrate(true)
            .defaultSchema(defaultSchema)
            .load();
        flyway.migrate();
        LOGGER.info("DB update finished.");
    }

    private static void createDatabase(
        final Connection connection,
        final String dbName,
        final Collation setCollation
    ) throws SQLException
    {
        final String createDbQuery = getCreateDbQuery(connection, dbName, setCollation);

        try (final Statement createDbStatement = connection.createStatement()) {
            createDbStatement.executeUpdate(createDbQuery);
            LOGGER.info("Created new database: {}", dbName);
        }
    }

    private static String getCreateDbQuery(final Connection connection, final String dbName, final Collation collation) throws SQLException
    {
        final String queryTemplate = collation != null
            ? CREATE_DATABASE_BASE + " " + WITH_COLLATION
            : CREATE_DATABASE_BASE;
        if (collation != null) {
            LOGGER.debug("Creating DB with collation: {}", collation.value());
        } else {
            LOGGER.debug("Creating DB with default collation.");
        }
        final String formattedQuery = "SELECT format($fmt$" + queryTemplate + "$fmt$, "
            + (collation != null ? "?, ?, ?" : "?") + ")";
        LOGGER.info("Create DB Query: {}", formattedQuery);
        try (final PreparedStatement getCreateDbQueryStatement = connection.prepareStatement(formattedQuery)) {
            getCreateDbQueryStatement.setString(1, dbName);
            if(collation != null) {
                getCreateDbQueryStatement.setString(2, collation.value());
                getCreateDbQueryStatement.setString(3, collation.value());
            }
            final ResultSet set = getCreateDbQueryStatement.executeQuery();
            set.next();
            return set.getString(1);
        }
    }

    private static boolean doesDbExist(
        final Connection connection,
        final String dbName
    ) throws SQLException
    {
        try (final PreparedStatement statement = connection.prepareStatement(DOES_DATABASE_EXIST)) {
            statement.setString(1, dbName);
            final ResultSet set = statement.executeQuery();
            set.next();
            return set.getBoolean(1);
        }
    }

    private static void logReceivedArgumentsIfDebug(
        final String dbHost,
        final int dbPort,
        final String dbName,
        final String username,
        final List<String> secretKeys,
        final String schema,
        final Collation collation
    )
    {
        LOGGER.debug("Arguments received"
            + " dbHost: {}"
            + " dbPort: {}"
            + " dbName: {}"
            + " username: {}"
            + " secretKeys: {}"
            + " schema: {}"
            + " collation: {}",
            dbHost, dbPort, dbName, username, secretKeys, schema, collation != null ? collation.value() : null);
    }
}
