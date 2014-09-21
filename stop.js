var data;//holds data to be parsed directly to json

/**
* This module handles data regarding a point.
* Note that a stop can be in multiple routes. That's why we had to create a seperate
* module for a stop
*/
function Stop(d) {
   data = d;
}


/**
* This method checks if this stop is in the given line
*/
Stop.prototype.isStopInLine = function(pointsInLine){
   var tDistance = 0.01;//100 metres
   for(var pIndex = 0; pIndex < pointsInLine.length; pIndex++){
      if(this.getDistance(pointsInLine[pIndex].point_lat, pointsInLine[pIndex].point_lon) < tDistance){
         return true;
      }
   }
   return false;
};

/**
* This method returns the data on this stop as a json object
*/
Stop.prototype.getData = function() {
   return data;
};

/**
* This function gets the distance in kilometres between this stop
* and the provided point
*/
Stop.prototype.getDistance = function(pointLat, pointLon) {
   var rad = Math.PI/180;
   var R = 6371; // km
   var dLat = (pointLat-data.stop_lat)*rad;
   var dLon = (pointLon-data.stop_lon)*rad;
   var lat1 = data.stop_lat*rad;
   var lat2 = pointLat*rad;

   var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
   var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
   var d = R * c;
   return d;
};

module.exports = Stop;
