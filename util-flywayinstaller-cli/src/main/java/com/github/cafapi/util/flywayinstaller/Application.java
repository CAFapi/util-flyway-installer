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

import java.sql.SQLException;
import java.util.concurrent.Callable;

import com.github.cafapi.util.flywayinstaller.exceptions.FlywayMigratorException;

import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, name = "util-flywayinstaller")
public final class Application implements Callable<Integer>
{

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
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @CommandLine.Option(
            names = {"-db.connection"},
            paramLabel = "<connectionString>",
            required = true,
            description = "Specifies the connection string to the database service. " +
                    "e.g. jdbc:postgresql://localhost:3307/"
    )
    public void checkDbUrl(final String url) {
        if (!url.matches("^jdbc:postgresql:\\/\\/.*\\/$")) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    String.format("Invalid value '%s' for option '-db.connection'.\n" +
                            "The format provided is incorrect. Here is an example: jdbc:postgresql://localhost:3307/", url));
        }
        connectionString = url;
    }
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
            defaultValue = "INFO",
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
        try {
            Migrator.migrate(allowDBDeletion, connectionString, username, password, dbName, logLevel);
        } catch (final FlywayMigratorException | SQLException e) {
            return 1;
        }
        return 0;
    }
}
