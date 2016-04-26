echo "Starting required containers"
docker-compose up
echo "Scaling webapp to 3 nodes"
docker-compose scale dynamo=3
