Inorder for you to use this API, you need to setup a Postgresql database.
Setup the database by running the following commands (the database password used here can be changed but will also require you to change it in the code):

    apt-get install postgresql
    su - postgres
    psql
    create database ma3map
    \c ma3map
    create table commute(id serial primary key, start_id varchar, destination_id varchar, processing_time double precision);
    create table commute_path(id serial primary key, score double precision, commute_id integer references commute(id));
    create table commute_step(id serial primary key, commute_path_id integer references commute_path(id), text varchar, sequence integer, start_id varchar, destination_id varchar, route_id varchar);
    create user ma3map with nosuperuser nocreatedb nocreaterole noinherit login encrypted password 'kj432@cF23pl&d';
    

Exit out of psql
Make sure /etc/postgresql/[VERSION}/main/pg_hba.conf has the following lines:

    # TYPE  DATABASE    USER        CIDR-ADDRESS          METHOD
    # "local" is for Unix domain socket connections only
    local   all         postgres                               ident
    # IPv4 local connections:
    host    all         all         127.0.0.1/32          md5 
    host    all         all         ::1/128               md5 
    # IPv6 local connections:
    #host    all         all         ::1/128               ident
