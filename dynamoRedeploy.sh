#!/bin/bash

mvn clean package > build.log
cp ./target/dprs-0.0.1.jar ./docker/dynamo/dprs-0.0.1.jar

eval $(docker-machine env --swarm swarm-node-01)
docker-compose down --rmi all --volumes --remove-orphans
docker-compose build

eval $(docker-machine env swarm-node-02)
docker-compose down --rmi all --volumes --remove-orphans
docker-compose build

eval $(docker-machine env swarm-node-03)
docker-compose down --rmi all --volumes --remove-orphans
docker-compose build


