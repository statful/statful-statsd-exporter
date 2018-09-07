package com.statful.senders;

import com.statful.domain.Event;
import reactor.core.publisher.Mono;

public interface MetricsSender<T extends Event> {
    Mono<Void> send(Iterable<T> events);
}
