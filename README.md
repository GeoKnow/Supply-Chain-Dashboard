# GeoKnow Supply Chain Dashboard and Supply Chain Data Generator (a.k.a. Simulator)

This is a multi-project sbt project that contains the Supply Chain Dashboard and the Supply Chain Data Generator (a.k.a. Simulator)

Initial prototype of the supply chain dashboard, a web application that allows the user to search, browse and to explore supply chain data.

![Screenshot](screenshot.png)

## Supply Chain Data Generator

### Configuration

The simulator configuration is done in the file `simulator/conf/application.conf`.

Pay special attention to the `Endpoint Configuration` section and adapt it to your needs:

```conf
#############################
# Endpoint Config
#############################
# local, http, virtuoso
simulator.endpoint.kind="virtuoso"
simulator.endpoint.url="http://dockerhost/sparql"
simulator.virtuoso.host="dockerhost"
simulator.virtuoso.port="1111"
simulator.virtuoso.user="dba"
simulator.virtuoso.password="dba"
```

### Running

To run the data generator at port 9001:

    sbt "project simulator" compile "run 9001"

### REST API Usage

see [Simulator RAML doc](raml/simulator.md)

In a typicall workflow you will call:

- `/run?interval=0`
- `/calculateMetrics`

## Supply Chain Dashboard

### Configuration

The dashboard configuration is done in the file `dashboard/conf/application.conf`.

Pay special attention to the `Endpoint Configuration` section and adapt it to your needs:

```conf
#############################
# Endpoint Configuration
#############################
# local, http, virtuoso
simulator.endpoint.kind="virtuoso"
simulator.endpoint.url="http://dockerhost/sparql"
simulator.virtuoso.host="dockerhost"
simulator.virtuoso.port="1111"
simulator.virtuoso.user="dba"
simulator.virtuoso.password="dba"
```

### Running

To run the dashboard at port 9000:

    sbt "project dashboard" compile "run 9000"

### Usage

In your browser go to [http://localhost:9000](http://localhost:9000)

## Requirements

- JDK 7 or later
- Virtuoso 7.1 or later

## Packaging

**todo**: section outdated, update required

### As tarball with start/stop scripts for Linux and Windows

- Execute `sbt "project dashboard" universal:package-zip-tarball`
- The package can be found in 'target/universal'

### As WAR Archive

- Execute `sbt "project dashboard" war`
- The package can be found in 'target'
