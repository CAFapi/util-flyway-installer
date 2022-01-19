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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafapi.util.flywayinstaller.exceptions.InvalidConnectionStringException;

public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private static final String CONNECTION_URL_REGEX = "^.*\\/\\/.+:\\d+\\/";
    private static final String DROP_DATABASE = "DROP DATABASE ?";
    private static final String CREATE_DATABASE = "CREATE DATABASE ?";
    private static final String DOES_DATABASE_EXIST =
            "SELECT EXISTS (SELECT datname FROM pg_catalog.pg_database WHERE lower(datname) = lower(?));";

    private Migrator()
    {
    }

    public static void migrate(
        final boolean allowDBDeletion,
        final String connectionString,
        final String dbName,
        final String username,
        final String password
    ) throws InvalidConnectionStringException, SQLException
    {
        logReceivedArgumentsIfDebug(allowDBDeletion, connectionString, dbName, username, password);

        LOGGER.info("Starting migration ...");

        try  {
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            dbSource.setUrl(checkAndConvertConnectionUrl(connectionString));
            dbSource.setUser(username);
            dbSource.setPassword(password);

            final boolean exists = checkDBExists(dbSource, dbName);
            LOGGER.debug("Database exists: {}", exists);
            if (!exists || allowDBDeletion) {
                resetOrCreateDatabase(dbSource, exists, dbName);
            }

            LOGGER.info("About to perform DB update.");
            final Flyway flyway = Flyway.configure()
                    .dataSource(dbSource)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            flyway.validate();
            LOGGER.info("DB update finished.");
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("Migration completed ...");
    }

    private static void resetOrCreateDatabase(
        final PGSimpleDataSource dbSource,
        final boolean exists,
        final String dbName) throws SQLException
    {
        final String savedUrl = dbSource.getUrl();
        dbSource.setUrl(dbSource.getUrl() + "postgres");
        LOGGER.debug(dbSource.getUrl());

        try (final Connection connection = dbSource.getConnection();
             final PreparedStatement deleteStatement = connection.prepareStatement(DROP_DATABASE);
             final PreparedStatement createStatement = connection.prepareStatement(CREATE_DATABASE);
        ) {
            if (exists) {
                LOGGER.info("force deletion has been specified.\nDeleting database {}", dbName);
                deleteStatement.setString(1, dbName);
                deleteStatement.executeUpdate();
                LOGGER.info("DELETED database: {}", dbName);
            }
            createStatement.setString(1, dbName);
            createStatement.executeUpdate();
            LOGGER.info("Created new database: {}", dbName);
        }
        dbSource.setUrl(savedUrl);
    }

    private static boolean checkDBExists(final PGSimpleDataSource dbSource, final String dbName) throws SQLException
    {
        try (final Connection connection = dbSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(DOES_DATABASE_EXIST)) {
            statement.setString(1, dbName);
            final ResultSet set = statement.executeQuery();
            set.next();
            return set.getBoolean(1);
        }
    }

    private static String checkAndConvertConnectionUrl(final String connectionUrl) throws InvalidConnectionStringException
    {
        final String connectionUrlWithSlashes = connectionUrl.replace("\\", "/");
        final Matcher matcher = Pattern.compile(CONNECTION_URL_REGEX).matcher(connectionUrlWithSlashes);
        if (!matcher.find()) {
            throw new InvalidConnectionStringException(connectionUrl);
        }
        return matcher.group(0);
    }

    private static void logReceivedArgumentsIfDebug(final boolean allowDBDeletion,
                                                    final String connectionString,
                                                    final String dbName,
                                                    final String username,
                                                    final String password)
    {
        LOGGER.debug("Arguments received"
                + " allowDBDeletion: {}\n"
                + " connectionString: {}\n"
                + " dbName: {}\n"
                + " username: {}\n"
                + " password: {}", allowDBDeletion, connectionString, dbName, username, password);
    }
}
