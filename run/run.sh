#!/bin/sh

[ -z $XMX ] && echo "XMX not set. " && exit 1;
[ -z $XMS ] && echo "XMS not set. " && exit 1;

exec java \
-Xms${XMS}m -Xmx${XMX}m \
-jar \
-DconfigurationPath=$CONFIG_PATH \
-Denvironment=$ENVIRONMENT \
/usr/opt/service/service.jar
