#!/bin/bash
mvn clean install
cp ./target/dprs-0.0.1.jar ./docker/dynamo/dprs-0.0.1.jar

docker-compose -f docker-compose-elk.yml down
docker-compose -f docker-compose-elk.yml build
docker-compose -f docker-compose-elk.yml up
