# kinetica-aws-lambda-ingest
This is a lambda function that lets you ingest data to a Kinetica database. This function uses [/insert/records/frompayload](https://www.kinetica.com/docs/api/rest/insert_records_frompayload_rest.html) Kinetia API endpoint with [Kinetica Java API](https://www.kinetica.com/docs/api/java/index.html).


## Build

To build the function, clone the repository and use maven: 

`mvn clean package`

## Installation

Create an AWS Lambda function in your AWS account using Java 8 or Java 11 runtime. More information about how to do that can be found here:

<https://docs.aws.amazon.com/lambda/latest/dg/getting-started.html>

Then upload `kinetica_aws_lambda-jar-with-dependencies.jar` file as the function code (this file is build by maven or can be [downloaded here](https://github.com/kineticadb/kinetica-aws-lambda-ingest/raw/master/target/kinetica_aws_lambda-jar-with-dependencies.jar)).

Handler class is: `com.gpudb.kinetica.aws.Handler::handleRequest`

## How to use it

This function expects JsonObject or JsonArray as an input, example events:

JsonObject:
```
{
  "key1": "value1",
  "key2": "value2",
  "key3": "value3"
}
```

JsonArray:

```
[
  {
    "key1": "value1",
    "key2": "value2",
    "key3": "value3"
  },
  {
    "key1": "value1",
    "key2": "value2",
    "key3": "value3"
  },
  {
    "key1": "value1",
    "key2": "value2",
    "key3": "value3"
  }
]
```

There are certain environment variables that have to or can be set to  to be set:

| Environment variable         | Description            | Required |
|:--- |:--- |:--- |
| request_table_name           | Name of the table into which the data will be inserted, in [schema_name.]table_name format, using standard [name resolution rules](https://www.kinetica.com/docs/concepts/tables.html#table-name-resolution). If the table does not exist, the table will be created using either an existing type_id or the type inferred from the payload, and the new table name will have to meet standard [table naming criteria](https://www.kinetica.com/docs/concepts/tables.html#table-naming-criteria). | Y        |
| request_create_table_options | Options used when creating the target table. Includes type to use. The other options match those in /create/table. The default value is an empty map ( \{\} ). More information about these can be found [here](https://www.kinetica.com/docs/api/rest/insert_records_frompayload_rest.html) | N        |
| request_options              | Optional parameters. The default value is an empty map ( \{\} ). More information about these can be found [here](https://www.kinetica.com/docs/api/rest/insert_records_frompayload_rest.html)                       | N        |
| kinetica_url                 | URL where Kinetica database is running, example: `http://my_kinetica.com:9191/` | Y        |
| kinetica_options             | Options to be used when making connection to kinetica database, these are explained in detail in the next table. Example: `{ "username":"admin", "password":"My_password"}` The default value is an empty map ( \{\} ).                        | N        |

kinetica_options environment variable can contain following options:

| Option                              | Description
|:--- |:---
| bypasssslcertcheck                    | Sets the flag indicating whether to verify the SSL certificate for HTTPS connections.
| clusterreconnectcount                 | Sets the number of times the API tries to reconnect to the same cluster (when a failover event has been triggered), before actually failing over to any available backup cluster.
| connectioninactivityvalidationtimeout | Sets the period of inactivity (in milliseconds) after which connection validity would be checked before reusing it.
| disableautodiscovery                  | Sets the value of the flag indicating whether to disable automatic discovery of backup clusters or worker rank URLs.
| disablefailover                       | Sets the value of the flag indicating whether to disable failover upon failures (both high availability--or inter-cluster--failover and N+1--or intra-cluster--failover.
| hafailoverorder                       | Sets the value of the enum controlling the inter-cluster (high availability) failover priority.
| hostmanagerport                       | Sets the host manager port number.
| hostnameregex                         | Sets the IP address or hostname regex against which the server's rank URLs would be matched when obtaining them.
| httpheaders                           | Replaces the contents of the map of additional HTTP headers to send to GPUdb with each request with the contents of the specified map.
| initialconnectionattempttimeout       | Sets the timeout used when trying to establish a connection to the database at GPUdb initialization.
| intraclusterfailovertimeout           | Sets the timeout used when trying to recover from an intra-cluster failover event.
| logginglevel                          | Sets the logging level that will be used by the API.
| maxconnectionsperhost                 | Sets the maximum number of connections, per host, allowed at any given time.
| maxtotalconnections                   | Sets the maximum number of connections, across all hosts, allowed at any given time.
| password                              | Sets the password to be used for authentication to GPUdb.
| primaryurl                            | Sets the URL of the primary cluster to use amongst the HA clusters.
| serverconnectiontimeout               | Sets the server connection timeout value, in milliseconds, after which an inability to establish a connection with the GPUdb server will result in requests being aborted.
| threadcount                           | Sets the number of threads that will be used during data encoding and decoding operations.
| timeout                               | Sets the timeout value, in milliseconds, after which a lack of response from the GPUdb server will result in requests being aborted.
| username                              | Sets the username to be used for authentication to GPUdb.
| usesnappy                             | Sets the flag indicating whether to use Snappy compression for certain GPUdb requests that potentially submit large amounts of data.

