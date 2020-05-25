package com.statful.senders.statful;

import com.statful.domain.DefaultEvent;
import com.statful.senders.MetricsSender;
import com.statful.client.domain.api.Aggregation;
import com.statful.client.domain.api.StatfulClient;
import com.statful.client.domain.api.Tags;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Map;
import java.util.Optional;

public class StatfulSender implements MetricsSender<DefaultEvent> {

    private static final Logger LOGGER = Loggers.getLogger(StatfulSender.class);

    private StatfulClient statfulClient;

    public StatfulSender(StatfulClient statfulClient) {
        this.statfulClient = statfulClient;
    }

    //TODO: Change this method to perhaps use an async udp sender. Udp send will only block if socket buffer is full
    public Mono<Void> send(Iterable<DefaultEvent> events) {
        return Mono.create(sink -> {
            try {
                events.forEach(event -> {
                    switch (event.getType()) {
                        case COUNTER:
                            statfulClient.sampledCounter(event.getMetricName(), Math.round(event.getValue()), Math.round(event.getSampleRate() * 100))
                                    .with()
                                    .tags(toTags(event.getTags()))
                                    .send();
                            break;
                        case GAUGE:
                            statfulClient.sampledGauge(event.getMetricName(), Math.round((double) event.getValue()), Math.round(event.getSampleRate() * 100))
                                    .with()
                                    .tags(toTags(event.getTags()))
                                    .aggregations(Aggregation.AVG, Aggregation.P90, Aggregation.P95, Aggregation.MAX, Aggregation.SUM, Aggregation.COUNT)
                                    .send();
                            break;
                        case TIMER:
                        case HISTOGRAM:
                            statfulClient.sampledTimer(event.getMetricName(), Math.round(event.getValue()), Math.round(event.getSampleRate() * 100))
                                    .with()
                                    .tags(toTags(event.getTags()))
                                    .aggregations(Aggregation.AVG, Aggregation.P90, Aggregation.P95, Aggregation.MAX, Aggregation.SUM, Aggregation.COUNT)
                                    .send();
                            break;
                        default:
                            LOGGER.warn("Unknown metric type");
                    }
                    LOGGER.debug("Sent metric to statful. {}", event);
                });
            } catch (Exception e) {
                sink.error(e);
            }
            sink.success();
        });
    }

    private Tags toTags(Map<String, String> tags) {
        return Optional.ofNullable(tags)
                .map(map -> map
                        .entrySet()
                        .stream()
                        .map(es -> Tags.from(es.getKey(), es.getValue()))
                        .reduce(Tags::merge)
                        .orElse(new Tags()))
                .orElse(new Tags());
    }
}
