/*
 * Copyright 2021-2024 Open Text.
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpe.caf.secret.SecretUtil;

import picocli.CommandLine;

@CommandLine.Command(
    name = "util-flywayinstaller",
    mixinStandardHelpOptions = true,
    exitCodeListHeading = "%nExit Codes:%n",
    exitCodeList = {" 0:Successful program execution.",
                    " 1:Execution exception: An exception occurred while executing the migrations."
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
        names = {"-db.secretKeys"},
        paramLabel = "<secretKeys>",
        required = true,
        split = ",",
        description = "Specifies the keys of the secret(s) used to access the database, separated by commas."
    )
    private List<String> secretKeys;

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
            Migrator.migrate(dbHost, dbPort, dbName, username, secretKeys, getFirstNonEmptySecret(secretKeys));
        } catch (final SQLException | RuntimeException | IOException ex) {
            LOGGER.error("Issue while migrating.", ex);
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

    private static String getFirstNonEmptySecret(final List<String> secretKeys) throws IOException
    {
        for (final String secretKey : secretKeys) {
            final String secretValue = SecretUtil.getSecret(secretKey);
            if (secretValue != null && !secretValue.isEmpty()) {
                return secretValue;
            }
        }

        throw new RuntimeException(String.format("No secret found for key(s): {}", secretKeys));
    }
}
