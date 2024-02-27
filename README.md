# Flyway Database Installer

The Flyway Database Installer is a generic  java application that will create and update databases using the supplied Flyway change log.

## Consuming the Installer
To make use of the installer use the following steps:

1. Create a new Java project.
2. Add `com.github.cafapi.util.flywayinstaller-cli` as a runtime dependency.
3. Create the migration directory `src/main/resources/db/migration`.

Note: The installer comes prepackaged only with Postgresql driver. If you need to use another database driver you must add it as a 
dependency to your own project.

    <dependencies>
        <dependency>
            <groupId>com.github.cafapi.util.flywayinstaller</groupId>
            <artifactId>util-flywayinstaller-cli</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>


## Running the Installer

The installer is run via command line. The following dependencies are expected in the classpath:

- logback-classic-1.4.14.jar
- logback-core-1.4.14.jar 
- util-flywayinstaller-2.1.0-SNAPSHOT.jar 
- flyway-core-10.7.1.jar 
- jackson-dataformat-toml-2.16.1.jar 
- jackson-databind-2.16.1.jar 
- jackson-annotations-2.16.1.jar 
- gson-2.10.1.jar 
- postgresql-42.7.1.jar 
- checker-qual-3.42.0.jar 
- flyway-database-postgresql-10.7.1.jar 
- jcl-over-slf4j-2.0.11.jar 
- picocli-4.7.5.jar 
- slf4j-api-2.0.11.jar 
- caf-logging-logback-2.0.0-238.jar 
- caf-logging-logback-converters-2.0.0-238.jar 
- jackson-core-2.16.1.jar 
- caf-logging-common-2.0.0-238.jar 
- util-process-identifier-3.0.0-549.jar 
- commons-text-1.11.0.jar 
- commons-lang3-3.14.0.jar

The following arguments can be specified:

*   `db.host`    : Specifies the host name.  e.g. localhost
*   `db.port`    : Specifies the server port.  e.g. 5432
*   `db.user`    : Specifies the username to access the database.
*   `db.pass`    : Specifies the password to access the database.
*   `db.name`    : Specifies the name of the database to be created or updated.
*   `log`        : Specifies the logging level of the installer. Valid options are: [DEBUG, INFO, WARNING, ERROR, OFF]

The command to run the jar will be in the following format:

    java -cp [system properties] <dependency-jars>:installer.jar com.github.cafapi.util.flywayinstaller.Application [arguments]

For example:

    java -cp *:myInstaller.jar com.github.cafapi.util.flywayinstaller.Application -db.host localhost -db.port 5437 -db.user postgres -db.pass postgres -db.name test-db2 -log DEBUG
