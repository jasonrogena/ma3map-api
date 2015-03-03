# ma3map
 
ma3map is a [matatu](http://en.wikipedia.org/wiki/Matatu) transit application for Nairobi. It uses GTFS data collected by University of Nairobi's C4DLab available [here](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/). For more information on the GTFS data refer to [Digital Matatus' website](http://www.digitalmatatus.com/). Visit ma3map's [GitHub Homepage](https://www.github.com/ma3map) for the entire codebase.

## API [![Build Status](https://travis-ci.org/ma3map/ma3map-api_nodejs.svg?branch=master)](https://travis-ci.org/ma3map/ma3map-api_nodejs)

The NodeJS API runs on [Heroku](https://www.heroku.com) as a Node.js app.

Setting up nodejs:
    
    cd ma3map-api_nodejs
    npm install fs
    npm install restify

    apt-get install postgresql-server-dev-9.1
    npm install pg


Running on localhost (make sure your are still in the project root dir):

    npm start


Deploying on heroku:

    heroku create --stack cedar ma3map
    git push heroku master
    heroku open
    heroku logs --tail


### Database
As you might have noticed in the deployment, we are not deploying any database. Instead we do the deployment manually using psql and a database dump. The deployed database is readable from anywhere in the interwebs but if you want to deploy your own database use the following commands:

    cd data/gis/clean
    chmod a+x ma3map.sql
    psql -h {host} -p {port} -U {username} -W<ma3map.sql
    cd ../../

Then modify the connection string in database.js with your credentials


## LICENSE 

This code is released under the [GNU Affero General Public License version 3](http://www.gnu.org/licenses/agpl-3.0.html). Please see the file LICENSE.md for details.
