var PG;//holds the postgres object

var connString;
/**
* This module wraps around the database.
* PostgreSQL is currently the DB of choice
*/
function Database() {
   PG = require('pg');
   connString = "postgres://yfjrluyomctphg:HvGZ3dmZbbbxwCthg1_a8__AQC@ec2-54-225-101-4.compute-1.amazonaws.com:5432/d6nvm9856p4su4?ssl=true";
}

/**
* This method is responsible for handling queries.
* As of now all queries by this application are read
* queries. This method is therefore optimized for that
*/
Database.prototype.runQuery = function(qString, context, postExecute) {

   PG.connect(connString, function(error, client, done){
      if(error != null){
         console.error("unable to connect to database", error);
         postExecute(context, new Array());
         return;
      }

      var query = client.query(qString, function(error, result){
         done();//release the client. very important

         if(error != null){
            console.error("error running query", error);
         }
      });
      query.on('end', function(data){
         postExecute(context, data.rows);
      });
      
   });
};

module.exports = Database;
