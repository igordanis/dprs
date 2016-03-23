FROM frolvlad/alpine-oraclejdk8:slim

MAINTAINER igordanis@yahoo.com

COPY ./docker/zabbix/zabbix_agentd.conf /etc/zabbix/zabbix_agentd.conf
COPY ./docker/node_entrypoint.sh /node_entrypoint.sh

RUN \
	apk update \
	&& apk add zabbix-agent=2.4.7-r1 \
	&& rm -rf /var/cache/apk/* \
	&& mkdir -p /etc/zabbix/zabbix_agentd.d

ADD ./target/dprs-0.0.1.jar app.jar

RUN sh -c 'touch /app.jar'

#otvorenie portov:
#	8080 	->	spring boot
#	10050	->	zabbix client
EXPOSE 8080 10050

ENTRYPOINT /node_entrypoint.sh