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

import com.github.cafapi.util.flywayinstaller.exceptions.InvalidConnectionStringException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private static final String CONNECTION_URL_REGEX = "^.*\\/\\/.+:\\d+\\/";
    private static final String CREATE_DATABASE = "CREATE DATABASE \"%s\"";
    private static final String DOES_DATABASE_EXIST
        = "SELECT EXISTS ( SELECT datname FROM pg_catalog.pg_database WHERE lower( datname ) = lower( ? ) );";

    private Migrator()
    {
    }

    public static void migrate(
        final String connectionString,
        final String dbName,
        final String username,
        final String password
    ) throws InvalidConnectionStringException, SQLException
    {
        logReceivedArgumentsIfDebug(connectionString, dbName, username, password);

        LOGGER.info("Starting migration ...");

        try {
            final PGSimpleDataSource dbSource = new PGSimpleDataSource();
            final String urlAfterConversion = checkAndConvertConnectionUrl(connectionString);
            dbSource.setUrl(urlAfterConversion);
            dbSource.setUser(username);
            dbSource.setPassword(password);

            if (!doesDbExist(dbSource, dbName)) {
                LOGGER.debug("reset or createDB");
                resetOrCreateDatabase(dbSource, dbName);
            }
            // Update dbSource url to include the database name
            dbSource.setUrl(urlAfterConversion + dbName);

            LOGGER.info("About to perform DB update.");
            final Flyway flyway = Flyway.configure()
                .dataSource(dbSource)
                .baselineOnMigrate(true)
                .load();
            flyway.migrate();
            LOGGER.info("DB update finished.");

        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        LOGGER.info("Migration completed ...");
    }

    private static void resetOrCreateDatabase(
        final PGSimpleDataSource dbSource,
        final String dbName
    ) throws SQLException
    {
        try (final Connection connection = dbSource.getConnection();
             final Statement statement = connection.createStatement();) {
            statement.executeUpdate(String.format(CREATE_DATABASE, dbName));
            LOGGER.info("Created new database: {}", dbName);
        }
    }

    private static boolean doesDbExist(
        final PGSimpleDataSource dbSource,
        final String dbName
    ) throws SQLException
    {
        try (final Connection connection = dbSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(DOES_DATABASE_EXIST)) {
            statement.setString(1, dbName);
            final ResultSet set = statement.executeQuery();
            set.next();
            return set.getBoolean(1);
        }
    }

    /**
     * We check the validity of the incoming connectionUrl then use the first section ex: jdbc:postgresql://localhost:5437/
     *
     * @param connectionUrl the connection url received
     * @return the first section of the connectionUrl if valid
     * @throws InvalidConnectionStringException
     */
    private static String checkAndConvertConnectionUrl(final String connectionUrl) throws InvalidConnectionStringException
    {
        final Matcher matcher = Pattern.compile(CONNECTION_URL_REGEX).matcher(connectionUrl);
        if (!matcher.find()) {
            throw new InvalidConnectionStringException(connectionUrl);
        }
        return matcher.group(0);
    }

    private static void logReceivedArgumentsIfDebug(
        final String connectionString,
        final String dbName,
        final String username,
        final String password
    )
    {
        LOGGER.debug("Arguments received"
            + " connectionString: {}"
            + " dbName: {}"
            + " username: {}"
            + " password: {}", connectionString, dbName, username, password);
    }
}
