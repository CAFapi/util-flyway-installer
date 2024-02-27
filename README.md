# Flyway Database Installer

The Flyway Database Installer is a generic  java application that will create and update databases using the supplied Flyway change log.

## Consuming the Installer
To make use of the installer use the following steps:

1. Create a new Java project.
2. Add `com.github.cafapi.util.flywayinstaller-cli` as a runtime dependency.
3. Create the migration directory `src/main/resources/db/migration`.

    <dependencies>
        <dependency>
            <groupId>com.github.cafapi.util.flywayinstaller</groupId>
            <artifactId>util-flywayinstaller-cli</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

Note: If you need to use a database driver other than `Postgresql` driver you must add it as a dependency to your project.

## Running the Installer

The installer is run via command line. The dependencies mentioned in the manifest file of the `util-flywayinstaller-cli` jar are expected in the classpath.

The following arguments can be specified:

*   `db.host`    : Specifies the host name.  e.g. localhost
*   `db.port`    : Specifies the server port.  e.g. 5432
*   `db.user`    : Specifies the username to access the database.
*   `db.pass`    : Specifies the password to access the database.
*   `db.name`    : Specifies the name of the database to be created or updated.
*   `log`        : Specifies the logging level of the installer. Valid options are: [DEBUG, INFO, WARNING, ERROR, OFF]

The command to run the jar will be in the following format:

    java -cp [system properties] *:installer.jar com.github.cafapi.util.flywayinstaller.Application [arguments]

For example:

    java -cp *:myInstaller.jar com.github.cafapi.util.flywayinstaller.Application -db.host localhost -db.port 5437 -db.user postgres -db.pass postgres -db.name test-db2 -log DEBUG
