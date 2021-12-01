/*
 * Copyright 2021 Micro Focus or one of its affiliates.
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafapi.util.flywayinstaller.exceptions.FlywayMigratorException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;


public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);

    private Migrator()
    {
    }

    public static void migrate(final boolean allowDBDeletion,
                               final String connectionString,
                               final String username,
                               final String password,
                               final String dbName,
                               final LogLevel logLevel) throws SQLException, FlywayMigratorException
    {
        setLogLevel(logLevel.toString());
        LOGGER.debug("Arguments received {} {} {} {} {} {}", allowDBDeletion, connectionString, username, password, dbName, logLevel);
        LOGGER.info("Starting migration ...");
        try (final BasicDataSource dbSource = new BasicDataSource()) {
            dbSource.setUrl(connectionString);
            dbSource.setUsername(username);
            dbSource.setPassword(password);

            final boolean exists = checkDBExists(dbSource, dbName);
            LOGGER.debug("Database exists: {}", exists);
            if (!exists || allowDBDeletion) {
                resetOrCreateDatabase(dbSource, exists, dbName);
            }

            LOGGER.info("About to perform DB update.");
            final Flyway flyway = Flyway.configure()
                    .dataSource(dbSource.getUrl(), username, password)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            LOGGER.info("DB update finished.");
        } catch (final SQLException e) {
            throw new FlywayMigratorException("Issue while trying to perform the migration.", e);
        }
        LOGGER.info("Migration completed ...");
    }

    private static void setLogLevel(final String logLevel)
    {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach(tmpLogger -> tmpLogger.setLevel(Level.toLevel(logLevel)));
        LOGGER.debug("Setting log level to {}", logLevel);
    }

    private static void resetOrCreateDatabase(final BasicDataSource dbSource, final boolean exists, final String dbName) throws SQLException
    {
        dbSource.setUrl(dbSource.getUrl() + "postgres");
        LOGGER.debug(dbSource.getUrl());
        try (final Connection c = dbSource.getConnection();
             final Statement statement = c.createStatement()
        ) {
            if (exists) {
                LOGGER.info("force deletion has been specified.\nDeleting database {}", dbName);
                statement.executeUpdate("DROP DATABASE " + dbName );
                LOGGER.info("DELETED database: {}", dbName);
            }
            statement.executeUpdate("CREATE DATABASE " + dbName);
            LOGGER.info("Created new database: {}", dbName);
        }
    }

    private static boolean checkDBExists(final BasicDataSource dbSource, final String dbName) throws SQLException
    {
        try (
                final Connection c = dbSource.getConnection();
                final Statement statement = c.createStatement()
        ) {
            final boolean exists = statement.executeQuery("SELECT * FROM pg_database WHERE datname=lower('" + dbName + "');").next();
            LOGGER.info("Database {} exists: {}", dbName, exists);
            return exists;
        }
    }

}
