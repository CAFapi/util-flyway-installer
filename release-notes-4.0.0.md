!not-ready-for-release!

#### Version Number
${version-number}

#### Breaking Changes
- US969005: The `db.pass` argument has been replaced by `db.secretKeys`.  
  - This program now expects to be passed secret keys rather than a secret value.
  - For example, pass `db.secretKeys CAF_SERVICE_DATABASE_PASSWORD` instead of `db.pass P@ssw0rd`. The program will then look up the actual
    secret using the key. If multiple secret keys are passed (`db.secretKeys CAF_SERVICE_DATABASE_PASSWORD,CAF_DATABASE_PASSWORD`), the
    first non-empty secret value will be used to access the database.
  - This change prevents secrets being exposed when calling this program. 

#### New Features

#### Known Issues