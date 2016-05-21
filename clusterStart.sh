#!/usr/bin/env bash
docker-machine start consul-machine \
    && docker-machine start swarm-node-01 \
    && docker-machine start swarm-node-02 \
    && docker-machine start swarm-node-03


