/*
 * Copyright 2021-2021 Micro Focus or one of its affiliates.
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


public final class Migrater
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrater.class);
    private Migrater()
    {
    }
    
    public static void migrate(final boolean allowDBDeletion,
                               final String fullConnectionString,
                               final String connectionString,
                               final String username,
                               final String password,
                               final String dbName ) throws SQLException
    {
        System.out.println("fullConnectionString "+fullConnectionString);
        final String connectionUrl = defineConnectionUrl(fullConnectionString, connectionString, dbName);
        System.out.println("connectionUrl "+ connectionUrl);
        boolean dbExists = checkDBExists(connectionUrl, username, password);
        
        if (dbExists && allowDBDeletion) {
            LOGGER.info("\n DB - Exists, and force deletion has been specified for: {}\n", dbName);
    
            final BasicDataSource basicDataSourceNoDB = new BasicDataSource();
            basicDataSourceNoDB.setUrl(connectionUrl);
            basicDataSourceNoDB.setUsername(username);
            basicDataSourceNoDB.setPassword(password);
            
            try (final Connection connection = basicDataSourceNoDB.getConnection();
                 final Statement statement = connection.createStatement()){
                statement.executeUpdate("DROP DATABASE " + dbName);
                LOGGER.info("DELETED database: {}", dbName);
                dbExists = false;
            }
        }
        if (!dbExists) {
            LOGGER.info("about to perform DB installation from scratch.");
    
            final BasicDataSource basicDataSourceNoDB = new BasicDataSource();
            basicDataSourceNoDB.setUrl(connectionUrl);
            basicDataSourceNoDB.setUsername(username);
            basicDataSourceNoDB.setPassword(password);
            
            try (final Connection c = basicDataSourceNoDB.getConnection();
                final Statement statement = c.createStatement()) {
                statement.executeUpdate("CREATE DATABASE " + dbName);
                LOGGER.info("Created new database: {}", dbName);
            }
        }
        LOGGER.info("About to perform DB update.");
        final Flyway flyway = Flyway.configure()
                .dataSource(connectionUrl, username, password)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        System.out.println("DB update finished.");
    }
    
    private static String defineConnectionUrl(final String fullConnectionString, final String connectionString, final String dbName)
    {
        if(fullConnectionString.isEmpty() && (connectionString.isEmpty() || dbName.isEmpty())){
            throw new RuntimeException("We should have either fullConnectionString or (connectionString and dbName)");
        }
        if(!fullConnectionString.isEmpty()){
            if(fullConnectionString.contains("?")){
                return fullConnectionString.substring(0, fullConnectionString.lastIndexOf('?')+1);
            }
            return fullConnectionString;
        } else {
            return connectionString +"/"+ dbName;
        }
    }
    
    private static boolean checkDBExists(final String connectionUrl, final String username,
                                         final String password) throws SQLException
    {
        try (final BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl(connectionUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            try (final Connection connection = dataSource.getConnection()) {
                return true;
            } catch (final Exception e) {
                return false;
            }
        }
    }
    
}

