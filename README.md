# GeoKnow Supply Chain Dashboard and Supply Chain Data Generator (a.k.a. Simulator)

This is a multi-project sbt project that contains the Supply Chain Dashboard and the Supply Chain Data Generator (a.k.a. Simulator)

Initial prototype of the supply chain dashboard, a web application that allows the user to search, browse and to explore supply chain data.

![Screenshot](screenshot.png)

## Requirements

- JDK 7 or later
- Virtuoso 7.1 or later 

## Configuration

### Data Generator

- configuration file is ``dashboard/conf/application.conf``

### Dashboard

- configuration file is ``simulator/conf/application.conf``



- Execute `sbt "project dashboard" run`
- In your browser, navigate to 'http://localhost:9000'

## Packaging

**todo**: section outdated, update required

### As tarball with start/stop scripts for Linux and Windows

- Execute `sbt "project dashboard" universal:package-zip-tarball`
- The package can be found in 'target/universal'

### As WAR Archive

- Execute `sbt "project dashboard" war`
- The package can be found in 'target'
