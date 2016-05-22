#!/usr/bin/env bash

eval $(docker-machine env --swarm swarm-node-01)
docker-compose down
docker-compose up -d

docker-compose scale dynamo="2"
docker-compose scale dynamo2="2"

cd ./chaos-testing/
sh ./initPropertyFile.sh
cd ..

docker-compose logs -f