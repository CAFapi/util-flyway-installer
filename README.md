# Flyway Database Installer

The Flyway Database Installer is a generic  java application that will create and update databases using the supplied Flyway change log.

## Running the Installer

The installer is run via command line. The migration files and dependencies of the `util-flywayinstaller-cli` jar are expected in the classpath.

The following arguments can be specified:

*   `db.host`        : Specifies the host name.  e.g. localhost
*   `db.port`        : Specifies the server port.  e.g. 5432
*   `db.user`        : Specifies the username to access the database.
*   `db.secretKeys`  : Specifies the secret keys used to access the database, separated by commas. The first non-empty secret value found for any of these keys will be used.
*   `db.name`        : Specifies the name of the database to be created or updated.
*   `db.schema`      : (Optional) Specifies the database schema to be used. If not specified, the default schema for the database will be used.
*   `db.setCollation`: (Optional) Specifies the collation setting when creating the database. Valid options for now are: [`C`, `EN_US_UTF8`]. If not specified, the database default collation will be used.
*   `log`            : Specifies the logging level of the installer. Valid options are: [DEBUG, INFO, WARNING, ERROR, OFF]

The command to run the jar will be in the following format, where the migration files are in `db/migration`:

    java -cp "*:db" com.github.cafapi.util.flywayinstaller.Application [arguments]

For example:

    java -cp "*:db" com.github.cafapi.util.flywayinstaller.Application -db.host localhost -db.port 5437 -db.user postgres -db.secretKeys CAF_SERVICE_DATABASE_PASSWORD -db.name test-db2 -db.schema test_schema -db.setCollation C -log DEBUG
