package com.statful.mapper;

import com.statful.domain.Event;

import java.util.Collection;

public interface MetricMapper<T extends Event> {
    Iterable<T> map(Iterable<String> metricLines);
}
