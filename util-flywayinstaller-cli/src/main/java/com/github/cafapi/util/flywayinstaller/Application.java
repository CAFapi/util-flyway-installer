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

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import picocli.CommandLine;

@CommandLine.Command(
    name = "util-flywayinstaller",
    mixinStandardHelpOptions = true,
    exitCodeListHeading = "%nExit Codes:%n",
    exitCodeList = {" 0:Successful program execution.",
                    " 1:Authentication exception: There was an issue with your authentication. " +
                            "Check your credentials, as well as the host and port provided",
                    " 2:Execution exception: An exception occurred while executing the migrations."
                    })
public final class Application implements Callable<Integer>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private Application()
    {
    }

    @CommandLine.Option(
        names = {"-db.host"},
        paramLabel = "<dbHost>",
        required = true,
        description = "Specifies the database host name. "
        + "e.g. localhost"
    )
    private String dbHost;

    @CommandLine.Option(
        names = {"-db.port"},
        paramLabel = "<dbPort>",
        required = true,
        description = "Specifies the database port to be used. "
        + "e.g. 5432"
    )
    private int dbPort;

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
        description = "Specifies the name of the database to be created or updated."
    )
    private String dbName;

    @CommandLine.Option(
        names = {"-log"},
        paramLabel = "<logLevel>",
        description = "Specifies the logging level of the installer. Can be DEBUG, INFO, WARNING, ERROR or OFF. Must be Uppercase"
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
            Migrator.migrate(dbHost, dbPort, dbName, username, password);
        } catch (final Exception ex) {
            final String errorMessage = ex.getMessage();
            if (
                errorMessage.matches("Connection(.*)refused(.*)") ||
                errorMessage.matches("(.*)password authentication failed(.*)") ||
                errorMessage.matches("(.*)connection attempt failed(.*)")
            ) {
                LOGGER.error("Issue while authenticating. {} / {}", ex.getClass(), errorMessage);
                return 1;
            }
            LOGGER.error("Issue while migrating. {} / {}", ex.getClass(), errorMessage);
            return 2;
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
