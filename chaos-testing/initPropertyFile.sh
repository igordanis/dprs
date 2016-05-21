#!/usr/bin/env bash

echo "#Autogenerated by ../../../initPropertyFile.sh" > ./src/main/resources/application.properties

node2ip=$(docker-machine env swarm-node-02 | grep DOCKER_HOST | cut -f 2 -d "=" |  sed "s/\"//g" | sed "s/^tcp:\/\///")
node2cert=$(docker-machine env swarm-node-02 | grep DOCKER_CERT_PATH | tr -d "\"" | cut -d "=" -f 2)
echo "swarm-node-02.ip=https://$node2ip" >> ./src/main/resources/application.properties
echo "swarm-node-02.cert=$node2cert" >> ./src/main/resources/application.properties

node3ip=$(docker-machine env swarm-node-03 | grep DOCKER_HOST | cut -f 2 -d "=" |  sed "s/\"//g" | sed "s/^tcp:\/\///")
node3cert=$(docker-machine env swarm-node-03 | grep DOCKER_CERT_PATH | tr -d "\"" | cut -d "=" -f 2)
echo "swarm-node-03.ip=https://$node3ip" >> ./src/main/resources/application.properties
echo "swarm-node-03.cert=$node3cert" >> ./src/main/resources/application.properties

loadbalancer=$(docker-compose ps | grep load-balancer | tr -s '[[:space:]]' | cut -d " " -f 4 | tr - " " | cut -d " " -f 1)
echo "loadbalancer.ip=$loadbalancer" >> ./src/main/resources/application.properties