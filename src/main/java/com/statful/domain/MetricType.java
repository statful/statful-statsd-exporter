package com.statful.domain;

public enum MetricType {
    COUNTER("c"),
    GAUGE("g"),
    TIMER("ms"),
    HISTOGRAM("h");

    private String type;

    MetricType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static MetricType fromString(String text) {
        for (MetricType b : MetricType.values()) {
            if (b.type.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown metric type.");
    }
}
