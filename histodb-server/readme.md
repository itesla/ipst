# HistoDB server

## Installation 
Put the histodb-server-<VERSION>-exec.jar in the designated folder.

Provide a valid SSL certificate (see key* configuration parameters)

## Configuration
Add a "histodb-server" section to the platform configuration setting the following parameters :

	- persistent : true/false
    - basedir : path to mapdb storage folder
	- host : server address
	- port : listening port
	- username : user login
	- password : user password
	- keyStoreType : SSL key store type (i.e. JKS)
	- keyStore : path of keystore file
	- keyStorePassword : keystore password
	- keyAlias: key alias
	- separator : csv separator char
	- locale : locale (i.e. fr-FR)	



## Startup
to start the server execute the following command:

java -jar histodb-server-<VERSION>-exec.jar