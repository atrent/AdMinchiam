#!/bin/bash

ssh \
	-L  *:9775:localhost:9774\
	-L  *:9776:192.168.42.243:80\
	-vvv  root@alix.vpn.atrent.it
