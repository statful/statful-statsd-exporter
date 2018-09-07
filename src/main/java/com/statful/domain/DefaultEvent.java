package com.statful.domain;

import java.util.Map;
import java.util.Objects;

public class DefaultEvent extends Event {

    private DefaultEvent(Builder builder) {
        this.metricName = builder.metricName;
        this.value = builder.value;
        this.tags = builder.tags;
        this.type = builder.type;
        this.sampleRate = builder.sampleRate;
    }

    public static Builder builder(final String metricName) {
        Objects.requireNonNull(metricName, "Metric name cannot be null");
        return new Builder(metricName);
    }

    public static class Builder {

        private String metricName;
        private float value;
        private Map<String, String> tags;
        private MetricType type;
        private float sampleRate;

        private Builder(final String metricName) {
            this.metricName = metricName;
        }

        public Builder value(float value) {
            this.value = value;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder type(MetricType type) {
            this.type = type;
            return this;
        }

        public Builder sampleRate(float sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public DefaultEvent build() {
            return new DefaultEvent(this);
        }
    }
}
