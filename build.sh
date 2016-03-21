#!/bin/bash

#compile, package
echo "Bulding application"
#mvn clean package > build.log

echo "Starting required containers"
docker-compose up -d
echo "Scaling webapp to 5 nodes"
docker-compose scale web=5

# echo "info.: zabbix takes approx. 60sec to boot. Please be patient and keep refreshing :)"



# identicku funkcionalitu ako tento bash script
# poskytuje aj docker-compose. odteraz je nim nahradene
# docker-compose.yml :)
# 
# #set variables
# NETWORK_NAME="dprs"
# IMAGE_NAME="dprs"
# NODE1="NODE1"
# NODE2="NODE2"
# NODE3="NODE3"

# function createZabbixContainers(){
# 	# create /var/lib/mysql as persistent volume storage
# 	docker run -d -v /var/lib/mysql --name zabbix-db-storage busybox:latest

# 	# start DB for Zabbix - default 1GB innodb_buffer_pool_size is used
# 	docker run \
#     	-d \
#     	--name zabbix-db \
#     	-v /backups:/backups \
#     	-v /etc/localtime:/etc/localtime:ro \
#     	--volumes-from zabbix-db-storage \
#     	--env="MARIADB_USER=zabbix" \
#     	--env="MARIADB_PASS=my_password" \
# 	    zabbix/zabbix-db-mariadb

# # start Zabbix linked to started DB
# 	docker run \
#    		-d \
#     	--name zabbix \
#     	-p 80:80 \
#     	-p 10051:10051 \
#     	-v /etc/localtime:/etc/localtime:ro \
#     	--link zabbix-db:zabbix.db \
#     	--env="ZS_DBHost=zabbix.db" \
#     	--env="ZS_DBUser=zabbix" \
#     	--env="ZS_DBPassword=my_password" \
#     	zabbix/zabbix-3.0:latest
# 		# wait ~60 seconds for Zabbix initialization
# 		# Zabbix web will be available on the port 80, 
# 		# Zabbix server on the port 10051
# }

# function createAppContainers(){

# 	#Creates image from docker file
# 	docker build -t $IMAGE_NAME .

# 	#run container and portforward only first 
# 	docker run --detach -p 8080:8080 --name="$NODE1" $IMAGE_NAME
# 	docker run --detach  --name="$NODE2" $IMAGE_NAME
# 	docker run --detach  --name="$NODE3" $IMAGE_NAME

# }
	
# createZabbixContainers()
# createAppContainers()
	
# #create network  so containers can ping each other
# docker network create --subnet=172.18.0.0/16 $NETWORK_NAME
# docker network connect --ip="172.18.0.2" $NETWORK_NAME $NODE1
# docker network connect --ip="172.18.0.3" $NETWORK_NAME $NODE2
# docker network connect --ip="172.18.0.4" $NETWORK_NAME $NODE3
# docker network connect --ip="172.18.0.10" $NETWORK_NAME zabbix
	 


