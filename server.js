/**
* Starting point. We should probably have named this Heimdall
* Using restify to handle REST requests
*/
function Server() {
   console.log("Initializing server");
   //refer to http://mcavage.me/node-restify/#Creating-a-Server
   server = this;

   //initialize restify
   var restify = require('restify');
   server.restify = restify.createServer({
      name: 'ma3map'
   });
   server.restify
      .use(restify.fullResponse())
      .use(restify.bodyParser());

   //initialize the api endpoints
   server.initGetStopsEndpoint();
   server.initGetRoutesEndpoint();
   var port = 3000;
   if(typeof process.env.PORT != 'undefined'){
      port = process.env.PORT;
   }

   server.restify.listen(port, function(){
      console.log("%s listening at %s", server.restify.name, server.restify.url);
   });

}

/**
* This module initializes the API endpoint responsible for:
*     - getting the list of routes
*/
Server.prototype.initGetRoutesEndpoint = function() {
   console.log("Initializing getRoutes endpoint");
   //if user does not specify any route, get all routes
   server.restify.get('/get/routes', function(req, res, next){
      //check if data already cached in file
      var fs = require("fs");
      var path = require("path");
      var tmpDir = path.join(process.cwd(), 'tmp/');
      
      if(!fs.existsSync(tmpDir)) {
         console.log("creating tmp directory");
         fs.mkdirSync(tmpDir);
      }
      var routeDataFile = path.join(process.cwd(), 'tmp/route_data.json');
      if(fs.existsSync(routeDataFile)){
         console.log("getting cached route data");
         
         fs.readFile(routeDataFile, function (error, data) {
            var bufferString = data.toString();
            res.send(JSON.parse(bufferString));
         });
      }
      else {
         console.log("getting route data from database");
         
         var Database = require('./database');
         var db = new Database();
         
         //object showing the number of routes done getting their shit together 
         var complete = {
            "count": 0,
            "size": 1,
            "serverRes": res,
            "data": new Array(),
            "tmpFile": routeDataFile
         };         
         db.runQuery("select stop_id, stop_name, stop_code, stop_desc, stop_lat, stop_lon, location_type, parent_station from \"gtfs_stops\"", {"complete": complete}, function(context, data){
            var Database = require('./database');
            var db = new Database();

            var context = {"stops": data, "complete": context.complete};
            db.runQuery("select * from \"gtfs_routes\"", context, function(context, data){
               var routes = new Array();
               var stops = context.stops;
               var complete = context.complete;
               var Route = require('./route');
               console.log("Gotten %s routes", data.length);
               console.log(" and %s stops", stops.length);
               complete.size = data.length;
               
               for(var rIndex = 0; rIndex < data.length; rIndex++){
                  //if(rIndex == 0)
                  var currRoute = new Route(data[rIndex], stops, complete);
               }
            });
         });
      }
      
   });

   //user specified a route
   /*server.restify.get('/get/routes/:id', function(req, res, next){
      console.log("getting all routes");
      
   });*/
};

/**
/**
* This module initializes the API endpoint responsible for:
*     - getting the list of stops
*/
Server.prototype.initGetStopsEndpoint = function() {
   console.log("Initializing getStops endpoint");
   //if user does not specify any route, get all routes
   server.restify.get('/get/stops', function(req, res, next){
      var Database = require('./database');
      var db = new Database();
      var complete = {};
      db.runQuery("select stop_id, stop_name, stop_code, stop_desc, stop_lat, stop_lon, location_type, parent_station from \"gtfs_stops\"", {"complete": complete}, function(context, data){
         res.send(data);
      });
   });
};

* This module initializes the API endpoint responsible for:
*     - getting the paths between points
*/
Server.prototype.initGetPathsEndpoint = function() {
   console.log("Initializing getPaths endpoint");
   //if user does not specify any route, get all routes
   server.restify.get('/get/paths', function(req, res, next){
      var http = require('http');
      var options = {
         host: 'api.ma3map.org',
         port: '8080',
         path: '/get-paths?from='+req.params.from+'&to='+req.params.to
      };
      var callback = function(response){
         var str = '';
         response.on('data', function(chunk){
            str += chunk;
         });
         response.on('end', function(){
            res.send(str);
         });
      }
      http.request(options, callback).end();
   });
};

/**
* This module initializes the API responsible for:
*     - searching the dataset and returning routes with hits
*/
Server.prototype.initSearchAPI = function() {
   server.restify.get('/search', function(req, res, next){
   });
};


/**
* This module initializes the API responsible for reporting bad:
*     - routes
*     - lines
*     - stops
*/
Server.prototype.initReportAPI = function() {
   server.restify.get('/report', function(req, res, next){
   });
};

var instance = new Server();
