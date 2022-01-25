# Flyway Database Installer

The Flyway Database Installer is a generic  java application that will create and update databases using the supplied Flyway change log.

## Consuming the Installer
To make use of the installer use the following steps:

1. Create a new Java project.
2. Add `com.github.cafapi.util.flywayinstaller` as a dependency.
3. Create the migration directory `src/main/resources/db/migration`.
4. Build the project with the mainClass as:

   `com.github.cafapi.util.flywayinstaller.Application`

This will produce a jar specific to your project.

Note: The installer comes prepackaged only with Postgresql driver. If you need to use another database driver you must add it as a 
dependency to your own project.

## Example POM
Below is an example project's POM showing how to build using the Flyway installer.

    <groupId>com.acme.anvils</groupId>
    <artifactId>anvil-db</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>com.github.cafapi.util.flywayinstaller</groupId>
            <artifactId>util-flywayinstaller</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.github.cafapi.util.flywayinstaller.Application</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id> <!-- this is used for inheritance merges -->
                        <phase>package</phase> <!-- bind to the packaging phase -->
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

## Running the Installer

The installer is run via command line. The following arguments can be specified:

*   `db.server`  : Specifies the server name.  e.g. localhost
*   `db.port`    : Specifies the server port.  e.g. 5432
*   `db.user`    : Specifies the username to access the database.
*   `db.pass`    : Specifies the password to access the database.
*   `db.name`    : Specifies the name of the database to be created or updated.
*   `log`        : Specifies the logging level of the installer. Valid options are: [DEBUG, INFO, WARNING, ERROR, OFF]

The command to run the jar will be in the following format:

    java -jar [system properties] installer.jar [arguments]

For example:

    java -jar myInstaller.jar -db.server=localhost -db.port=5437 -db.user=postgres -db.pass=postgres -db.name=test-db2 -log=DEBUG
