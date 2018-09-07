package com.statful.domain;

import java.util.Map;
import java.util.Objects;

public abstract class Event {
    protected String metricName;
    protected float value;
    protected Map<String, String> tags;
    protected MetricType type;
    protected float sampleRate;

    public String getMetricName() {
        return metricName;
    }

    public float getValue() {
        return value;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public MetricType getType() {
        return type;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Float.compare(event.value, value) == 0 &&
                Float.compare(event.sampleRate, sampleRate) == 0 &&
                Objects.equals(metricName, event.metricName) &&
                Objects.equals(tags, event.tags) &&
                type == event.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricName, value, tags, type, sampleRate);
    }

    @Override
    public String toString() {
        return "BaseEvent{" +
                "metricName='" + metricName + '\'' +
                ", value=" + value +
                ", tags=" + tags +
                ", type=" + type +
                ", sampleRate=" + sampleRate +
                '}';
    }
}
