# HistoDB server


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



## Startup/Shutdown
To start/stop the server use the "histodb-service.sh" script in platform bin folder:

	histodb-service.sh {start|stop|restart|status}

## Service interface
The Histodb server exposes an HTTP interface to request data loading and to query historical data.

### Authentication
The server requires basic HTTP authentication with username and password.

### Load data
The histodb loads networks data from a specified local path on the histodb server host.
The load command can be given making an HTTP POST request to the URL

	https://<host>:<port>/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla

setting the following parameter:

	- dir :  local folder containing the network files to load 

Example using wget command:

	wget --user=username --password password https://<host>:<port>/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla --post-data='dir=/caserepo/IIDM'

The load of many network files can take a log time to complete. 
If using wget command be careful to specify a congruous timeout to avoid wget's default behavior of retrying the same request up to 20 times every 900 seconds (default timeout). 
For example:

	wget --timeout 200000 --user=username --password password https://<host>:<port>/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/ --post-data='dir=/caserepo/test/IIDM'
	
To avaoid self-signed SSL certificate validation failures eventually add the "--no-check-certificate" wget option.

### Set reference network
When loading a set of network data (snapshots and forecasts) the last loaded snapshot will be kept as reference for the list of network attributes.
However it is possible to load a new reference network making a HTTP POST or PUT to the following URL:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/referenceCIM

setting the following parameter:

	- dir :  path to the snapshot network file 

### Get Reference Network
To retrieve the current reference networkId make an HTTP GET the following URL:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/referenceCIM

## Query
To query historical data it is possible to make an HTTP GET request passing the query parameters on the query string, e.g.:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data.<format>?<param1>=<value1>&<param2>=<value2>..
	

### Response format
Data can be formatted either as CSV file or as compressed CSV file specifying the requested format in the request URL as follows:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data.<format>?<param1>=<value1>&<param2>=<value2>..
	
where format=cvs|zip

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data.csv?<param1>=<value1>&<param2>=<value2>..
	
	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data.zip?<param1>=<value1>&<param2>=<value2>..
 

### Get historical data 
Historical data can be queried using the following URL :

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data.<format>?<param1>=<value1>&<param2>=<value2>...

### Get forecast diff
To retrieve forecasts with matching snapshot use the following URL:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/data/forecastsDiff.<format>?<param1>=<value1>&<param2>=<value2>...
	
### Get statistics
To get statistics use the following URL:

	/histodb/rest/itesladb/<datasource_prefix>/<datasource_postfix>/itesla/stats.<format>?<param1>=<value1>&<param2>=<value2>...
