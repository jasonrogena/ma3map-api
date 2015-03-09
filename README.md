# ma3map
 
ma3map is a [matatu](http://en.wikipedia.org/wiki/Matatu) transit application for Nairobi. It uses GTFS data collected by University of Nairobi's C4DLab available [here](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/). For more information on the GTFS data refer to [Digital Matatus' website](http://www.digitalmatatus.com/). Visit ma3map's [GitHub Homepage](https://www.github.com/ma3map) for the entire codebase.

## API

The Java API runs as a [Jersey Web Service](https://jersey.java.net) running ontop of [The Grizzly NIO Framework](https://grizzly.java.net). This API exposes the following endpoints:

1. /get_paths
---------

Use this endpoint to get alternative Matatu routes from point-a to point-b in Nairobi. This endpoint expects two GET Request parameters ('from' and 'to') e.g:

    **[DEPLOYMENT IP ADDRESS]**:9998/get_paths?from=-1.264945,36.721226&to=-1.279868,36.818099

### Deployment

This [Maven](https://maven.apache.org) project is very easy to deploy. Make sure you have the following dependencies are installed:

    openjdk-7-jdk
    maven

Make sure you use OpenJDK 7 and not 6 or 8. Also make sure you point your JAVA_HOME to the right directory.

Deployment is as easy as:

    git clone https://github.com/ma3map/ma3map-api_java.git
    cd ma3map-api_java 
    mvn clean install
    mvn exec:java

This API has already been deployed in a Linode instance (212.111.43.103). The paths endpoint, for instance, can be accessed by calling:

    http://212.111.43.103:9998/get_paths?from=-1.264945,36.721226&to=-1.279868,36.818099

