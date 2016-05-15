#!/bin/sh
echo $(hostname -i | cut -d ' ' -f1)
/bin/consul agent -server -config-dir=/config -advertise $(hostname -i | cut -d ' ' -f1) -data-dir /tmp/consul -bootstrap  -client 0.0.0.0 -node consulservers
