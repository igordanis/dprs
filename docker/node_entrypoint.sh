#!/bin/sh
echo "	- starting zabbix client"
zabbix_agentd -c /etc/zabbix/zabbix_agentd.conf
echo "	- starting webapp"
java -Djava.security.egd=file:/dev/./urandom -jar /app.jar