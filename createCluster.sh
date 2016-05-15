#!/usr/bin/env bash

docker-machine rm -f consul-machine
docker-machine rm -f swarm-master
docker-machine rm -f swarm-node-01
docker-machine rm -f swarm-node-02
docker-machine rm -f swarm-node-03
docker network rm -f swarm-network

docker-machine create -d=virtualbox consul-machine
eval $(docker-machine env consul-machine)
docker-compose -f docker-compose-consul.yml up -d


docker-machine create -d virtualbox --swarm --swarm-master \
    --swarm-discovery="consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-store=consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-advertise=eth1:2376" \
    swarm-node-01

docker-machine create -d virtualbox --swarm \
    --swarm-discovery="consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-store=consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-advertise=eth1:2376" \
    swarm-node-02

docker-machine create -d virtualbox --swarm \
    --swarm-discovery="consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-store=consul://$(docker-machine ip consul-machine):8500" \
    --engine-opt="cluster-advertise=eth1:2376" \
    swarm-node-03


alias switch-01="eval $(docker-machine env swarm-node-01)"
alias switch-02="eval $(docker-machine env swarm-node-02)"
alias switch-03="eval $(docker-machine env swarm-node-03)"
alias switch-swarm="eval $(docker-machine env --swarm swarm-node-01)"

#switch-swarm
eval $(docker-machine env --swarm swarm-node-01)
docker network create --driver overlay --subnet=10.0.9.0/24 swarm-network

