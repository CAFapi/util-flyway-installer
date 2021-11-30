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
                               final String dbName) throws SQLException, FlywayMigratorException
    {
        try (final BasicDataSource dbSource = new BasicDataSource()) {
            final String fullConnectionUrl = getFullConnectionUrl(fullConnectionString, connectionString, dbName);

            dbSource.setUrl(fullConnectionUrl);
            dbSource.setUsername(username);
            dbSource.setPassword(password);

            final boolean exists = checkDBExists(dbSource);
            if (!exists || allowDBDeletion) {
                resetOrCreateDatabase(dbSource, exists);
            }

            LOGGER.info("About to perform DB update.");
            final Flyway flyway = Flyway.configure()
                    .dataSource(dbSource.getUrl(), username, password)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            LOGGER.info("DB update finished.");
        } catch (final FlywayMigratorException e) {
            LOGGER.error("Issue while trying to perform the upgrade.", e);
        }
    }

    private static void resetOrCreateDatabase(final BasicDataSource dbSource, final boolean exists) throws SQLException
    {
        final String dbName = dbSource.getUrl().substring(dbSource.getUrl().lastIndexOf("/") + 1);

        LOGGER.info("\nDB {}- does not exist, or force deletion has been specified for it.\n", dbName);
        
        final String url = dbSource.getUrl().substring(0, dbSource.getUrl().lastIndexOf("/") + 1);
        dbSource.setUrl(url);
        
        try (final Connection c = dbSource.getConnection();
             final Statement statement = c.createStatement()
        ) {
            if (exists) {
                statement.executeUpdate("DROP DATABASE " + dbName);
            }
            LOGGER.info("DELETED database: {}", dbName);
            statement.executeUpdate("CREATE DATABASE " + dbName);
            LOGGER.info("Created new database: {}", dbName);
        }
    }

    private static String getFullConnectionUrl(
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

    private static boolean checkDBExists(final BasicDataSource dbSource)
    {
        try (final Connection ignored = dbSource.getConnection()) {
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
