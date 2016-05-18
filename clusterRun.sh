#!/usr/bin/env bash
docker-machine start consul-machine
docker-machine start swarm-node-01
docker-machine start swarm-node-02
docker-machine start swarm-node-03

switch-01
switch-02
switch-03
switch-swarm

docker-compose up

docker-compose scale dynamo="3"
docker-compose scale dynamo1="3"
