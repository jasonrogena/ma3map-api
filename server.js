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

   //initialize the api listeners
   server.initGetAPI();
   var port = 3000;
   if(typeof process.env.PORT != 'undefined'){
      port = process.env.PORT;
   }

   server.restify.listen(port, function(){
      console.log("%s listening at %s", server.restify.name, server.restify.url);
   });

}

/**
* This module initializes the API responsible for:
*     - getting the list of routes
*     - getting all route data
*     - getting a single route's data
*/
Server.prototype.initGetAPI = function() {
   console.log("Initializing get API");
   //if user does not specify any route, get all routes
   server.restify.get('/get/routes', function(req, res, next){
      console.log("getting all routes");
      var Database = require('./database');
      var db = new Database();

      var complete = {"count": 0, "size": 1, "serverRes": res, "data": new Array()};//object showing the number of routes done getting their shit together 
      
      db.runQuery("select stop_id, stop_name, stop_code, stop_desc, stop_point[0] as stop_lat, stop_point[1] as stop_lon, location_type, parent_station from stops", {"complete": complete}, function(context, data){
         var Database = require('./database');
         var db = new Database();

         var context = {"stops": data, "complete": context.complete};
         db.runQuery("select * from routes", context, function(context, data){
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
      
   });

   //user specified a route
   /*server.restify.get('/get/routes/:id', function(req, res, next){
      console.log("getting all routes");
      
   });*/
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
