FROM frolvlad/alpine-oraclejdk8:slim

MAINTAINER igordanis@yahoo.com

COPY ./config/dynamo/application.properties /application.properties
COPY ./config/rsyslog /etc/rsyslog.d
COPY ./config/zabbix/zabbix_agentd.conf /etc/zabbix/zabbix_agentd.conf
COPY ./entrypoint.sh /entrypoint.sh
ADD ./dprs-0.0.1.jar app.jar

RUN \
	apk add --no-cache zabbix-agent=2.4.8-r0 \
	&& rm -rf /var/cache/apk/* \
	&& mkdir -p /etc/zabbix/zabbix_agentd.d \
	&& apk add --no-cache rsyslog

# Download consul
ADD https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip /tmp/consul.zip
RUN cd /bin && unzip /tmp/consul.zip && chmod +x /bin/consul && rm /tmp/consul.zip
RUN mkdir -p /etc/consul

RUN sh -c 'touch /app.jar'

#otvorenie portov:
#	8080 	->	spring boot
#	10050	->	zabbix client
EXPOSE 8080 10050

ENTRYPOINT /entrypoint.sh