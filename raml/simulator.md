# REST Interfaces

Here we gives an overview of all available HTTP interfaces of the Supply Chain Data Generator.

## Base URL

All relative API URLs are prefixed by a base URL of the form `https://{domain}:{port}` with the following parameters:

- `domain`: The hostname or domain, where SCD is running. (required: true, type: string)

- `port`: The port on which the application server is available. (required: false, type: integer)

## SPARQL query via HTTP GET (/sparql)
SPARQL query via HTTP GET and query parameters

### /sparql - SPARQL query via HTTP GET

Valid HTTP methods are:

#### GET

##### Query Parameters

This method accept the following query parameters:

* **query** *( required )*:

    * SPARQL query via HTTP GET and query parameters

    * type: (string)

##### Response

The expected response:

* HTTPCode: [200](http://httpstatus.es/200):

    * TODO

    * **text/turtle **:
  

  
  
        * example :
            `TODO
`
  

## Simulator Status (/status)
Shows the current simulator status

### /status - Simulator Status

Valid HTTP methods are:

#### GET

##### Response

The expected response:

* HTTPCode: [200](http://httpstatus.es/200):

    * **application/json **:
  

  
  
        * example :
            `{
  "status":"Ready for simulation.",
  "simulationStartDate":"2010-01-01T00:00:00.000+01:00",
  "simulationEndDate":"2014-12-31T00:00:00.000+01:00",
  "simulationCurrentDate":"2010-01-01T00:00:00.000+01:00",
  "configuration":{
      "endpointConfiguration":{
          "doInit":true,
          "kind":"virtuoso",
          "defaultGraph":"http://www.xybermotive.com/geoknow/",
          "defaultGraphWeather":"http://www.xybermotive.com/GeoKnowWeather#",
          "defaultGraphConfiguration":"http://www.xybermotive.com/configuration/",
          "url":"",
          "host":"docker.local",
          "port":"1111",
          "user":"dba",
          "password":"dba"
      },
      "silkUrl":"http://localhost:9002/",
      "silkProject":"supplychainmetrics",
      "silkTask":"metrics",
      "productUri":"http://www.xybermotive.com/products/Car",
      "minStartDate":"2010-01-01T00:00:00.000+01:00",
      "maxEndDate":"2014-12-31T00:00:00.000+01:00",
      "tickIntervalsDays":1.0,
      "orderIntervalDays":1.0,
      "orderCount":10
  }
}
`
  

## Run simulation (/run)
Runs the simulation with the given parameters.

### /run - Run simulation

Valid HTTP methods are:

#### POST

##### Query Parameters

This method accept the following query parameters:

* **start** :

    * Defines the simulation start date. To be provided in format `YYYY-MM-DD`. Defaults to simulator.minStartDate in the `simulator/conf/application.conf`.

    * type: (date)

* **end** :

    * Defines the simulation end date. To be provided in format `YYYY-MM-DD`. Defaults to simulator.maxEndDate in the `simulator/conf/application.conf`.

    * type: (date)

* **interval** :

    * Defines the interval in seconds between two simulation ticks. Defaults to `1.0`.

    * type: (number)

* **graphUri** :

    * Defines the graph where generated data will be inserted into. Defaults to simulator.defaultGraph="http://www.xybermotive.com/geoknow/" as set in the `simulator/conf/application.conf`.

    * type: (string)

* **productUri** :

    * Defines root product URI to be used in the simulation. Defaults to simulator.product.uri="http://www.xybermotive.com/products/Car" as set in the `simulator/conf/application.conf`

    * type: (string)

##### Response

The expected response:

* HTTPCode: [200](http://httpstatus.es/200):

    * **text/plain **:
  

  
  
        * example :
            `run`
  

## Pause simulation (/pause)
Pauses the simulation.

### /pause - Pause simulation

Valid HTTP methods are:

#### POST

##### Response

The expected response:

* HTTPCode: [200](http://httpstatus.es/200):

    * **text/plain **:
  

  
  
        * example :
            `pause`
  

## Calculate metrics (/calculateMetrics)
Pre-calculates the metrics upon the generated data. Shall be runned after `run`.

### /calculateMetrics - Calculate metrics

Valid HTTP methods are:

#### POST

##### Query Parameters

This method accept the following query parameters:

* **graphUri** :

    * Defines the graph where generated data will be inserted into. Defaults to simulator.defaultGraph="http://www.xybermotive.com/geoknow/" as set in the `simulator/conf/application.conf`.

    * type: (string)

* **productUri** :

    * Defines root product URI to be used in the simulation. Defaults to simulator.product.uri="http://www.xybermotive.com/products/Car" as set in the `simulator/conf/application.conf`

    * type: (string)

##### Response

The expected response:

* HTTPCode: [200](http://httpstatus.es/200):

    * **text/plain **:
  

  
  
        * example :
            `metrics`
  

* HTTPCode: [503](http://httpstatus.es/503):

    * **text/plain **:
  

  
  
        * example :
            `Simulation is running, can not calculate performance metrics now. Retry later.`
  

