#!/usr/bin/env bash
docker-machine start consul-machine
docker-machine start swarm-node-01
docker-machine start swarm-node-02
docker-machine start swarm-node-03

alias switch-01="eval $(docker-machine env swarm-node-01)"
alias switch-02="eval $(docker-machine env swarm-node-02)"
alias switch-03="eval $(docker-machine env swarm-node-03)"
alias switch-swarm="eval $(docker-machine env --swarm swarm-node-01)"

switch-01
switch-02
switch-03
switch-swarm

docker-compose up

docker-compose scale dynamo="3"
docker-compose scale dynamo1="3"
