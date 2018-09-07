package com.statful.receivers.udp;

import com.statful.config.UdpConfiguration;
import com.statful.domain.Event;
import com.statful.mapper.MetricMapper;
import com.statful.senders.MetricsSender;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Arrays;
import java.util.Objects;

public class UdpServer {

    private static final Logger LOGGER = Loggers.getLogger(UdpServer.class);

    public static <T extends Event> void start(final UdpConfiguration config,
                                               final MetricMapper<T> metricMapper,
                                               final MetricsSender<T> metricsSender) {
        Objects.requireNonNull(config, "StatsdStatfulExporterConfiguration object must not be null");

        /*
           SO_RCVBUF	The size of the socket receive buffer
         */

        reactor.ipc.netty.udp.UdpServer udpServer = reactor.ipc.netty.udp.UdpServer
                .create(config.getHost(), config.getPort());

        udpServer.startAndAwait((udpInbound, udpOutbound) -> udpInbound
                .receive()
                .asByteArray()
                .map(String::new)
                .map(data -> metricMapper.map(Arrays.asList(data.split("\n"))))
                .flatMap(metricsSender::send)
                .flatMap(d -> udpOutbound.then())
                .doOnError(e -> LOGGER.error("Error processing metrics", e)));
    }

}
