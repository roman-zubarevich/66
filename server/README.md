# The 66 server

To run locally, start a local instance of CouchDB and make sure its port and database name are correctly specified in **couchdb.dev.properties** file. Run the server with `-Dprofile=dev` VM option.
To specify the database password, create a file **secret.properties** in the working directory containing `couchdb.password` property with the correct value.
