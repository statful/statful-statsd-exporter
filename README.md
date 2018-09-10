Statsd metric exporter
======================

Reactor Netty based application that listens to metrics over tcp and udp and sends transformed metrics via UDP.
Main goal is to map satsd metric format into [Statful](https://www.statful.com/) format. The implementation allows customization of metric senders, mappers and receivers. Meaning that,
you can easily create a new sender other than statful, by implementing MetricsSender. Check StatfulSender for an example.

Inspired on: https://github.com/prometheus/statsd_exporter

    +----------+                         +-------------------+                        +--------------+
    |  StatsD  |---(UDP/TCP repeater)--->|  statsd_exporter  |--(UDP relay metrics)-->|  Relay       |
    +----------+                         +-------------------+                        +--------------+

Refer to Envoy metric documentation to make sense of the example below [Envoy Metrics](https://www.envoyproxy.io/docs/envoy/latest/configuration/cluster_manager/cluster_stats)

Configuration
=============
An example configuration file

    tcp:
      port: 8080
    udp:
      port: 8125
      host: 127.0.0.1
    mappings:
      - match: envoy.cluster.*.canary.upstream_rq_time
        name: "http.canary.requests.upstream"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_active
        name: "active_requests"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_pending_active
        name: "pending_active_requests"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_pending_total
        name: "pending_total_requests"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_cx_active
        name: "active_connections"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.membership_healthy
        name: "membership_healthy"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.membership_total
        name: "membership_total"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_timeout
        name: "requests_timeout"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_total
        name: "requests_total"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_cx_http1_total
        name: "total_http1_requests"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_cx_http2_total
        name: "total_http2_requests"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_time
        name: "http.requests.upstream"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.external.upstream_rq_time
        name: "http.external.requests.upstream"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.internal.upstream_rq_time
        name: "http.internal.requests.upstream"
        tags:
          upstream: "$3"
      - match: envoy.cluster.*.upstream_rq_[0-9]+(xx)?
        name: "http.requests.upstream"
        tags:
          upstream: "$3"
          status_code: "$4"
      - match: envoy.cluster.*.external.upstream_rq_[0-9]+(xx)?
        name: "http.external.requests.upstream"
        tags:
          upstream: "$3"
          status_code: "$5"
      - match: envoy.cluster.*.internal.upstream_rq_[0-9]+(xx)?
        name: "http.internal.requests.upstream"
        tags:
          upstream: "$3"
          status_code: "$5"
      - match: envoy.cluster.*.canary.upstream_rq_[0-9]+(xx)?
        name: "http.canary.requests.upstream"
        tags:
          upstream: "$3"
          status_code: "$5"
      - match: "*"
        action: DROP
    statful:
      flushInterval: 5000
      flushSize: 10
      dryRun: false
      host: localhost
      port: 2013
      app: statsdexporter
      namespace: statsdexporter
      environment: local

Mappings match incoming metrics and are applied sequentially, stopping when a match is found. If no match is found, the metric is sent as is.
A Match can be viewed as a regex that will be applied to an incoming metric, where wildcard `*` is replaced by `[a-zA-Z_]+`  
When a match occurs, the metric will then be split by `.` and $X will get the value associated with the according position.

Mappings have two actions available: DROP or MATCH. The default is MATCH.

e.g: 

    - match: envoy.cluster.*.upstream_rq_[0-9]+(xx)?
      name: "http.requests.upstream"
      tags:
        upstream: "$3"
        status_code: "$4"
    
    for metric: envoy.cluster.cluster_hold_auth.upstream_rq_2xx
    
    will yield the following metric:
    
    metric: http.requests.upstream
    tags:
      upstream: "cluster_hold_auth"
      status_code: "upstream_rq_2xx"

NOTE: Be careful with regex that match more specific matches. Since its a first match found within the mappings list,
you should put more generic mappings at the bottom of the list.

Build
=====
    mvn clean install

Run
=====
    java -jar target/statsd-statful-exporter-*-jar-with-dependencies.jar /optional/absolute/path/to/config
    
If no path to external config is provided, by default the app looks for /tmp/config.json
    
Docker
======

    docker build -t statsdexporter .
    docker run -p 8125:8125/udp \ 
    -p 8080:8080> \ 
    -e CONFIG_PATH=/tmp/config.yaml \
    -e ENVIRONMENT=MY_ENV \
    -v $PWD/.config/config.yaml:/tmp/config.yaml \
    statsdexporter:latest

TODO
====
- Tests
- Improve documentation
- Release artifacts

Maintainer
==========
hugo.barrigas@mindera.com
