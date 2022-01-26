/*
 * Copyright 2022 Micro Focus or one of its affiliates.
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
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private static final String CREATE_DATABASE = "SELECT format('CREATE DATABASE %I', ?)";
    private static final String DOES_DATABASE_EXIST
        = "SELECT EXISTS (SELECT NULL FROM pg_catalog.pg_database WHERE lower( datname ) = lower( ? ));";

    private Migrator()
    {
    }

    public static void migrate(
        final String dbHost,
        final int dbPort,
        final String dbName,
        final String username,
        final String password
    ) throws SQLException
    {
        logReceivedArgumentsIfDebug(dbHost, dbPort, dbName, username, password);

        LOGGER.info("Checking connection ...");

        final PGSimpleDataSource dbSource = new PGSimpleDataSource();
        dbSource.setServerNames(new String[]{dbHost});
        dbSource.setPortNumbers(new int[]{dbPort});
        dbSource.setUser(username);
        dbSource.setPassword(password);

        try (final Connection connection = dbSource.getConnection()) {
            if (!doesDbExist(connection, dbName)) {
                LOGGER.debug("reset or createDB");
                createDatabase(connection, dbName);
            }
        }
        LOGGER.info("Connection Ok. Starting migration ...");

        // Once we made sure that the database exists, we add the information in the dataSource
        dbSource.setDatabaseName(dbName);

        final Flyway flyway = Flyway.configure()
            .dataSource(dbSource)
            .baselineOnMigrate(true)
            .load();
        flyway.migrate();
        LOGGER.info("DB update finished.");
    }

    private static void createDatabase(
        final Connection connection,
        final String dbName
    ) throws SQLException
    {
        final String createDbQuery = getCreateDbQuery(connection, dbName);

        try (final PreparedStatement createDbStatement = connection.prepareStatement(createDbQuery)) {
            createDbStatement.executeUpdate();
            LOGGER.info("Created new database: {}", dbName);
        }
    }

    private static String getCreateDbQuery(final Connection connection, final String dbName) throws SQLException
    {
        try (final PreparedStatement getCreateDbQueryStatement = connection.prepareStatement(CREATE_DATABASE)) {
            getCreateDbQueryStatement.setString(1, dbName);
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
        final String password
    )
    {
        LOGGER.debug("Arguments received"
            + " dbHost: {}"
            + " dbPort: {}"
            + " dbName: {}"
            + " username: {}"
            + " password: {}", dbHost, dbPort, dbName, username, password);
    }
}
