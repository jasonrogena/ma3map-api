# ma3map [![Build Status](https://travis-ci.org/ma3map/ma3map-server.svg?branch=master)](https://travis-ci.org/ma3map/ma3map-server)
 
This is my atttempt in putting [Nairobi matatu transit route data](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/) by [Digital Matatus](http://www.digitalmatatus.com/) to good use

## Server
The server runs on [Heroku](https://www.heroku.com) as a Node.js app
Setting up nodejs:
    
    cd server
    npm install fs
    npm install restify

    apt-get install postgresql-server-dev-9.1
    npm install pg


Running the server (make sure your are still in the server dir)

    npm start


Deploying on heroku:

    heroku create --stack cedar ma3map
    git push heroku master
    heroku open
    heroku logs --tail


### Database
As you might have noticed in the server deployment, we are not deploying any database. Instead we do the deployment manually using psql and a database dump. The deployed database is readable from anywhere in the interwebs but if you want to deploy your own database use the following commands:

    cd data/gis/clean
    chmod a+x ma3map.sql
    psql -h {host} -p {port} -U {username} -W<ma3map.sql
    cd ../../

Then modify the connection string in database.js with your credentials


## LICENSE 

This code is released under the [GNU Affero General Public License version 3](http://www.gnu.org/licenses/agpl-3.0.html). Please see the file LICENSE.md for details.
