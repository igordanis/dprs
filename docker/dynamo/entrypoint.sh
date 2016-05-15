#!/bin/sh

ifconfig

echo "  - starting rsyslog daemon"
rsyslogd -f /etc/rsyslog.d/rsyslog.conf

#echo "	- starting zabbix client"
#zabbix_agentd -c /etc/zabbix/zabbix_agentd.conf

echo " - starting consul client"
echo "binding to: $(hostname -i | cut -d ' ' -f1)"
consul agent -data-dir /tmp/consul -join consulserver \
    -bind $(hostname -i | cut -d ' ' -f1) \
    -advertise $(hostname -i | cut -d ' ' -f1) >> /etc/consul/log &

echo "	- starting webapp"
java -Djava.security.egd=file:/dev/./urandom -jar /app.jar

cat  /etc/consul/log


