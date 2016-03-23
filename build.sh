#!/bin/bash

#compile, package
echo "Bulding application"
#mvn clean package > build.log

echo "Starting required containers"
docker-compose up -d
echo "Scaling webapp to 5 nodes"
docker-compose scale web=5

# echo "info.: zabbix takes approx. 60sec to boot. Please be patient and keep refreshing :)"