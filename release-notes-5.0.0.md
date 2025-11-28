!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes
- D1058125: The admin database used to connect for existence check and creation now defaults to 'postgres' if not supplied via the `adminDbName` parameter.  
Previously it defaulted to a database with the same name as the user name used to connect to the server.

#### New Features
- D1058125: Add optional `adminDbName` parameter.  
The value of this is the name of the admin database which is used for the initial database connection for existence checks and creation. Defaults to 'postgres' if not provided.

#### Known Issues
- None
