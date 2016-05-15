#!/bin/bash

/usr/local/bin/consul-template -config /etc/consul-template/config.d \
                               -wait 2s:20s \
                               -consul consulserver:8500
