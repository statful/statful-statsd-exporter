FROM java:8-jdk-alpine

WORKDIR statsd-statful-exporter

ARG TCP_PORT=8080
ARG UDP_PORT=8125
ARG XMX=400
ARG XMS=300

ENV XMX $XMX
ENV XMS $XMS

ADD run/run.sh run.sh

# Create app directory
RUN mkdir -p /usr/opt/service

# Bundle app source
COPY target/statsd-statful-exporter*-jar-with-dependencies.jar /usr/opt/service/service.jar

EXPOSE $TCP_PORT
EXPOSE $UDP_PORT/udp

ENTRYPOINT ["sh", "./run.sh"]
