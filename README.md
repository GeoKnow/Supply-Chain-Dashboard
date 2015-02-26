# GeoKnow Supply Chain Dashboard

Initial prototype of the supply chain dashboard, a web application that allows the user to search, browse and to explore supply chain data.

![Screenshot](screenshot.png)

# Building

The following commands are to be executed from the apps folder.

## Requirements

- JDK 7 or later
- Virtuoso 7.1 or later 

## Loading Weather data into virtuoso

- gunzip data/ncdc/ncdc-ghcnd_2010-2013.nt.gz
- load .nt file into virtuoso 
	- via conductor frontend using IRI: http://www.xybermotive.com/GeoKnowWeather#
	- via isql: ``DB.DBA.TTLP_MT (file_to_string_output ('/path/to/data.nt'), '', 'http://www.xybermotive.com/GeoKnowWeather#');`` (path need to be allowed in virtuoso.ini)
 
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
