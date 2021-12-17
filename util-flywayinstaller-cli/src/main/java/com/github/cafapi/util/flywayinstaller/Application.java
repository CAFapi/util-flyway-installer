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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.cafapi.util.flywayinstaller.exceptions.FlywayMigratorException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, name = "util-flywayinstaller")
public final class Application implements Callable<Integer>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private Application()
    {
    }

    @CommandLine.Option(
        names = {"-fd"},
        paramLabel = "<allowDBDeletion>",
        defaultValue = "false",
        description = "Enables the deletion of existing database for a fresh install."
    )
    private boolean allowDBDeletion;

    @CommandLine.Option(
        names = {"-db.connection"},
        paramLabel = "<connectionString>",
        required = true,
        description = "Specifies the connection string to the database service. "
        + "e.g. jdbc:postgresql://localhost:3307/"
    )
    private String connectionString;

    @CommandLine.Option(
        names = {"-db.user"},
        paramLabel = "<username>",
        required = true,
        description = "Specifies the username to access the database."
    )
    private String username;

    @CommandLine.Option(
        names = {"-db.pass"},
        paramLabel = "<password>",
        required = true,
        description = "Specifies the password to access the database."
    )
    private String password;

    @CommandLine.Option(
        names = {"-db.name"},
        paramLabel = "<dbName>",
        required = true,
        defaultValue = "",
        description = "Specifies the name of the database to be created or updated."
    )
    private String dbName;

    @CommandLine.Option(
        names = {"-log"},
        paramLabel = "<logLevel>",
        description = "Specifies the logging level of the installer. Can be DEBUG, INFO, WARNING or ERROR."
    )
    private LogLevel logLevel;

    public static void main(final String[] args)
    {
        final int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call()
    {
        if (logLevel != null) {
            setLogLevel(logLevel);
        }

        try {
            Migrator.migrate(allowDBDeletion, connectionString, dbName, username, password);
        } catch (final FlywayMigratorException e) {
            return 1;
        }
        return 0;
    }

    private static void setLogLevel(final LogLevel logLevel)
    {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLoggerList().forEach(tmpLogger -> tmpLogger.setLevel(Level.toLevel(logLevel.name())));
        LOGGER.debug("Log level set to {}.", logLevel);
    }
}
