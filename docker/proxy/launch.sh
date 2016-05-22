#!/bin/bash

/usr/local/bin/consul-template -config /etc/consul-template/config.d \
                               -wait 0s:0s \
                               -consul consulserver:8500