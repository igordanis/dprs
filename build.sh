#!/bin/bash

#compile, package
mvn clean package

#remove old docker image
docker rmi docker rmi fiit/dprs

#build new snapshot
docker build -t "fiit/dprs" .

#run container and portforward
docker run -d -p 8080:8080 fiit/dprs