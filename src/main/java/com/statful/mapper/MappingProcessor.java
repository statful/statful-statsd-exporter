package com.statful.mapper;

import com.statful.domain.Event;

public interface MappingProcessor<T extends Event> {
    T process(T e);
    boolean processable(String metric);
}
