# ma3map

ma3map is a [matatu](http://en.wikipedia.org/wiki/Matatu) transit application for Nairobi. It uses GTFS data collected by University of Nairobi's C4DLab available [here](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/). For more information on the GTFS data refer to [Digital Matatus' website](http://www.digitalmatatus.com/). Visit ma3map's [GitHub Homepage](https://www.github.com/ma3map) for the entire codebase.

## API

The Java API runs as a [Jersey Web Service](https://jersey.java.net) running ontop of [The Grizzly NIO Framework](https://grizzly.java.net). This API maps the route data into a [Neo4J](https://neo4j.com) database for faster querying and best-path determination.

This API exposes the following endpoints:

/get-paths
---------

Use this endpoint to get alternative Matatu routes from point-a to point-b in Nairobi. This endpoint expects GET Request parameters e.g:

    **[DEPLOYMENT IP ADDRESS]**:8080/get_paths?from=-1.264945,36.721226&to=-1.279868,36.818099

Here's a list of parameters that can be consumed by this endpoint:

* **from**: GPS coordinate representing the starting point for the commute
* **to**: GPS coordinate representing the final denstination for the commute
* **no_from_stops**: Optional. Number of matatu stops closest to **from** to be considered as the first stop in the commute
* **no_to_stops**: Optional. Number of matatu stops closest to **to** to be considered as the last stop in the commute

This endpoint previously relied on an implementation of [Dijkstra's Algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm). However, it solely relies on the Neo4J database for best-path determination.

### Deployment

This [Maven](https://maven.apache.org) project is very easy to deploy. Make sure you have the following dependencies are installed:

    openjdk-7-jdk
    maven
    postgresql postgresql-contrib postgis postgresql-9.3-postgis-2.1

Make sure you use OpenJDK 7 and not 6 or 8. Also make sure you point your JAVA_HOME to the right directory.

Bulding the project is as easy as:

    git clone https://github.com/ma3map/ma3map-api_java.git
    cd ma3map-api_java
    mvn clean install

Start the webserver by running the following commands (the first command sets the heap size available to Maven, adjust to your liking):

    export MAVEN_OPTS=-Xmx1024m
    mvn exec:java

If you want the API to be accessible from other hosts, make sure port 8080 is open for incoming TCP connections. Add the following rule to your IPv4 iptables rules file:

    -A INPUT -p tcp -m state --state NEW -m tcp --dport 8080 -j ACCEPT

This API has already been deployed in a Digital Ocean instance (46.101.42.136). The paths endpoint, for instance, can be accessed by calling:

    http://46.101.42.136:8080/ma3map/get-paths?from=-1.264945,36.721226&to=-1.279868,36.818099
