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

import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafapi.util.flywayinstaller.exceptions.FlywayMigratorException;


public final class Migrator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    
    private Migrator()
    {
    }
    
    public static void migrate(final boolean allowDBDeletion,
                               final String fullConnectionString,
                               final String connectionString,
                               final String username,
                               final String password,
                               final String dbNameInput) throws SQLException, FlywayMigratorException
    {
        final String dbName;
        try (final BasicDataSource dbSource = new BasicDataSource()) {
            final String fullConnectionUrl = defineDatabaseProperties(fullConnectionString, connectionString, dbNameInput);
            dbSource.setUrl(fullConnectionUrl.substring(0, fullConnectionUrl.lastIndexOf("/") + 1));
            dbName = fullConnectionUrl.substring(fullConnectionUrl.lastIndexOf('/') + 1);
            dbSource.setUsername(username);
            dbSource.setPassword(password);
            boolean dbExists = checkDBExists(dbSource);
            if (!dbExists || allowDBDeletion) {
                LOGGER.info("\nDB {}- does not exist, or force deletion has been specified for it.\n", dbName);
                
                try (final Connection c = dbSource.getConnection();
                     final Statement statement = c.createStatement()) {
                    statement.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
                    LOGGER.info("DELETED database: {}", dbName);
                    statement.executeUpdate("CREATE DATABASE " + dbName);
                    LOGGER.info("Created new database: {}", dbName);
                }
            }
            LOGGER.info("About to perform DB update.");
            final Flyway flyway = Flyway.configure()
                    .dataSource(dbSource.getUrl() + dbName, username, password)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            LOGGER.info("DB update finished.");
        } catch (FlywayMigratorException e) {
            LOGGER.error("Issue while trying to perform the upgrade.", e);
        }
    }
    
    private static String defineDatabaseProperties(
            final String fullConnectionString,
            final String connectionString,
            final String dbName) throws FlywayMigratorException
    {
        if (fullConnectionString.isEmpty() && (connectionString.isEmpty() || dbName.isEmpty())) {
            throw new FlywayMigratorException("We should have either fullConnectionString or (connectionString and dbName)");
        }
        if (!fullConnectionString.isEmpty()) {
            return fullConnectionString.contains("?") ?
                    fullConnectionString.substring(0, fullConnectionString.lastIndexOf('?') + 1) :
                    fullConnectionString;
        } else {
            return connectionString + "/" + dbName;
        }
    }
    
    private static boolean checkDBExists(final BasicDataSource dbSource) throws SQLException
    {
        try (final BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl(dbSource.getUrl());
            dataSource.setUsername(dataSource.getUsername());
            dataSource.setPassword(dataSource.getPassword());
            try (final Connection ignored = dataSource.getConnection()) {
                return true;
            } catch (final Exception e) {
                return false;
            }
        }
    }
    
}

