var data;//h/lds the data to be directly passed into json
var stops;//holds raw data for all stops
var complete;
var routeComplete;
var routeData;

function Line (d, s, c, rC, rD){
   data = d;
   stops = s;
   complete = c;
   routeComplete = rC;
   routeData = rD;
   
   this.getPoints();
}

Line.prototype.getPoints = function() {
   var Database = require('./database');
   var db = new Database();
   var context = {
      "data": data, 
      "stops": stops, 
      "complete": complete, 
      "routeComplete": routeComplete,
      "routeData": routeData}; 
   
   db.runQuery("select shape_point[0] as point_lat, shape_point[1] as point_lon, shape_sequence as point_sequence, shape_dist_traveled as dist_traveled from shapes WHERE shape_id = '"+data.line_id+"' order by shape_sequence", context, function(context, dbData){
      var data = context.data;
      var stops = context.stops;
      var complete = context.complete;
      var routeComplete = context.routeComplete;
      var routeData = context.routeData;
      console.log("line %s has %s points", data.line_id, dbData.length);      

      data.points = new Array();
      for(var pIndex = 0; pIndex < dbData.length; pIndex++){
         data.points.push(dbData[pIndex]);
      }
      
      data.stops = new Array();

      var Stop = require('./stop');
      for(var sIndex = 0; sIndex < stops.length; sIndex++){
         var currStop = new Stop(stops[sIndex]);
         
         if(currStop.isStopInLine(data.points)){
            data.stops.push(currStop.getData());
         }      
      }
      console.log("line %s has %s stops", data.line_id, data.stops.length);
      routeData.lines.push(data);
      routeComplete.count++;
      
      if(routeComplete.count == routeComplete.size){//done getting data for all the lines in the curr route
         complete.count++;
         complete.data.push(routeData);
         if(complete.count == complete.size){//check if all routes are done getting all their data
            console.log("done getting data for %s routes", complete.count);
            complete.serverRes.send(complete.data);
         }
      }
 
   });
};

module.exports = Line;
