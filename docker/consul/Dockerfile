FROM gliderlabs/consul-agent:0.6
ADD ./config /config/
ADD ./entrypoint.sh /tmp/consul2/entrypoint.sh
ENTRYPOINT ["/bin/consul", "agent", "-server", "-config-dir=/config"]
