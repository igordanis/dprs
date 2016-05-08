#!/bin/bash
mvn clean package > build.log
cp ./target/dprs-0.0.1.jar ./docker/dynamo/dprs-0.0.1.jar
docker-compose build dynamo
docker-compose up
