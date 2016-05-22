#!/usr/bin/env bash
docker-machine start consul-machine
docker-machine start swarm-node-01
docker-machine start swarm-node-02
docker-machine start swarm-node-03

docker-machine regenerate-certs consul-machine -f
docker-machine regenerate-certs swarm-node-01 -f
docker-machine regenerate-certs swarm-node-02 -f
docker-machine regenerate-certs swarm-node-03 -f