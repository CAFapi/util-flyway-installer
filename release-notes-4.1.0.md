#### Version Number
${version-number}

#### New Features
- **US1054145**: The installer now accepts an optional new parameter `db.schema` to specify the database schema to be used.  
  If not specified, the default schema for the database will be used.
- **US1053061**: The installer now accepts an optional new parameter `db.collation` to set collation when creating the database.  
  Valid options are: [C, UTF_8]. If not specified, the database default collation will be used.

#### Known Issues
- None
