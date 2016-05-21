#!/usr/bin/env bash

echo "Starting failover..." > ./failover.log

while true
do
    docker-compose scale dynamo="2"  >> ./failover.log
    docker-compose scale dynamo2="2" >> ./failover.log
    sleep 5
done
