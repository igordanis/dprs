#!/bin/bash

#compile, package
echo "Bulding application"
mvn clean package > build.log
cp ./target/dprs-0.0.1.jar ./docker/dynamo/dprs-0.0.1.jar
docker-compose build

echo "Starting required containers"
docker-compose up -d
echo "Scaling webapp to 5 nodes"
docker-compose scale dynamo=3

# echo "info.: zabbix takes approx. 60sec to boot. Please be patient :)"