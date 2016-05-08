#!/bin/sh
echo "  - starting rsyslog daemon"
rsyslogd -f /etc/rsyslog.d/rsyslog.conf

echo "	- starting zabbix client"
zabbix_agentd -c /etc/zabbix/zabbix_agentd.conf

echo " - starting consul client"
consul agent -data-dir /tmp/consul -join consulserver >> /etc/consul/log &

echo " - starting elasticsearch"
./elasticsearch/bin/elasticsearch -d

echo "	- starting webapp"
java -Djava.security.egd=file:/dev/./urandom -jar /app.jar

