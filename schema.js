var FS = require("fs");
FS.readFile('data/gis/ma3map.sql', 'utf8', function(err, dump){
   if(err){
      return console.error(err);
   }
   
   var PG = require ('pg').native;

   var connString = "postgres://ma3map:ma3map@localhost/ma3map";
   if(typeof process.ENV.DATABASE_URL == 'undefined'){
      var connString = process.ENV.DATABASE_URL;
   }

   console.log("connected to "+ connString);

   var client = PG.Client  (connString);
   client.connect();
   
   var query = client.query(dump);
   query.on('end', function() { 
      client.end();
      console.log("Finished dumping data into the database");
   });
});
