# ma3map

This is my atttempt in putting [Nairobi matatu transit route data](http://www.gtfs-data-exchange.com/agency/university-of-nairobi-c4dlab/) by [Digital Matatus](http://www.digitalmatatus.com/) to good use

## Server
The server runs on [Heroku](https://www.heroku.com) as a Node.js app
Setting up nodejs:
    
    cd server

    npm install restify

    apt-get install postgresql-server-dev-9.1
    npm install pg

Running the server (make sure your are still in the server dir)
    
    npm start
