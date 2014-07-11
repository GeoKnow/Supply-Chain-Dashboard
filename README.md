# GeoKnow Supply Chain Dashboard

Initial prototype of the supply chain dashboard, a web application that allows the user to search, browse and to explore supply chain data.

![Screenshot](screenshot.png)

# Building

The following commands are to be executed from the apps folder.

## Requirements

- JDK 7 or later
 
## Running

- Execute `sbt "project dashboard" run`
- In your browser, navigate to 'http://localhost:9000'

## Packaging

### As tarball with start/stop scripts for Linux and Windows

- Execute `sbt "project dashboard" universal:package-zip-tarball`
- The package can be found in 'target/universal'

### As WAR Archive

- Execute `sbt "project dashboard" war`
- The package can be found in 'target'
