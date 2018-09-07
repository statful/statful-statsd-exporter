package com.statful.mapper;

import com.statful.domain.DefaultEvent;
import com.statful.domain.MetricType;
import com.statful.exceptions.InvalidStatsDLineException;
import com.statful.exceptions.MalformedMetricComponentException;
import com.statful.exceptions.UnknownMetricTypeException;
import com.google.common.collect.ImmutableMap;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class DefaultMetricMapper implements MetricMapper<DefaultEvent> {

    private static final Logger LOGGER = Loggers.getLogger(DefaultMetricMapper.class);
    private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_.]");
    private static final Pattern BEGINS_WITH_DIGITS = Pattern.compile("^[0-9]");
    private static final String GAUGE_METRIC_TYPE = MetricType.GAUGE.getType();
    private static final String COUNTER_METRIC_TYPE = MetricType.COUNTER.getType();

    private static final float DEFAULT_SAMPLE_RATE = 1f;
    private static final String METADATA_ERRORS = "metadata_errors";

    private MappingProcessor<DefaultEvent> mappingProcessor;

    public DefaultMetricMapper(MappingProcessor<DefaultEvent> mappingProcessor) {
        this.mappingProcessor = mappingProcessor;
    }

    @Override
    public Iterable<DefaultEvent> map(Iterable<String> metricLines) {
        if (!metricLines.iterator().hasNext()) {
            return Collections.emptyList();
        }

        try {
            return StreamSupport.stream(metricLines.spliterator(), false)
                    .filter(ml -> mappingProcessor.processable(this.validLine(ml)[0]))
                    .map(line -> this.processLine(line, new ArrayList<>()))
                    .map(evs -> evs.stream().map(mappingProcessor::process)
                            .collect(toList()))
                    .flatMap(Collection::stream)
                    .collect(toList());

        } catch (Exception e) {
            LOGGER.error(String.format("Error processing metric lines: %s", metricLines), e);
            return Collections.singletonList(this.buildEvent(COUNTER_METRIC_TYPE, "batch_error",
                    1, DEFAULT_SAMPLE_RATE, null));
        }
    }

    /**
     * process statsd line and map it to an event. The processing itself originates various metric events,
     * that's why a list is used to store every event.
     *
     * @param line   - statsd line, e.g: glork:320|ms|@0.1|#tag:tag_value,another_tag:tag_value
     * @param events - metric events originating from the line
     * @return metric @Event
     */
    private List<DefaultEvent> processLine(final String line, final List<DefaultEvent> events) {

        try {
            //validate line and get its parts
            final String[] elements = this.validLine(line);
            final String metric = this.escapeString(elements[0]);
            final String metricMetadata = elements[1];

            LOGGER.debug("Processing valid line {}", line);

            //validate metric metadata and get its components
            final String[] components = this.validLineMetadata(metricMetadata);

            final String valueStr = components[0];
            final String statType = components[1];
            final Map<String, String> tags = new HashMap<>();

            float value = 0.0f;
            float samplingFactor = 1;

            try {
                value = Float.parseFloat(valueStr);
            } catch (NumberFormatException e) {

                events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS, 1f, DEFAULT_SAMPLE_RATE,
                        ImmutableMap.of("malformed_value", "malformed_value")));

                LOGGER.debug(String.format("Bad value %s on line: %s", valueStr, line));
            }

            //case where there's sampling and/or statsdDog values
            if (components.length >= 3) {

                List<String> filteredComponents = Arrays.stream(Arrays.copyOfRange(components, 2, components.length))
                        .filter(c -> {
                            if (c.length() == 0) {
                                LOGGER.debug("Empty metadata component on line: {}", line);

                                events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS,
                                        1f, DEFAULT_SAMPLE_RATE,
                                        ImmutableMap.of("malformed_metadata", "malformed_metadata")));

                                return false;
                            }
                            return true;
                        })
                        .collect(toList());

                for (String component : filteredComponents) {
                    switch (component.charAt(0)) {
                        case '@':
                            if (Arrays.stream(MetricType.values()).noneMatch(mt -> mt.getType().equals(statType))) { //for now we only support timers and counter

                                events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS,
                                        1f, DEFAULT_SAMPLE_RATE,
                                        ImmutableMap.of("illegal_metric_type", "illegal_metric_type")));

                                LOGGER.debug(String.format("Illegal metric type on line: %s", line));
                            }

                            try {
                                samplingFactor = Float.parseFloat((component.substring(1, component.length())));
                            } catch (Exception e) {

                                events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS,
                                        1f, DEFAULT_SAMPLE_RATE,
                                        ImmutableMap.of("invalid_sample_factor", "invalid_sample_factor")));

                                LOGGER.debug(String.format("Invalid sampling factor %s on line: %s", component, line), e);
                            }

                            break;
                        case '#':
                            tags.putAll(this.parseDogStatsDTags(component));
                            break;
                        default:

                            events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS,
                                    1f, DEFAULT_SAMPLE_RATE,
                                    ImmutableMap.of("unknown_metadata_component_type", "unknown_metadata_component_type")));

                            LOGGER.debug("Invalid sampling factor or tag section {} on line {}", components[2], line);
                    }
                }
            }

            try {
                events.add(buildEvent(statType, metric, value, samplingFactor, tags));
                events.add(this.buildEvent(GAUGE_METRIC_TYPE, "valid_lines",
                        1f, DEFAULT_SAMPLE_RATE, null));
            } catch (Exception e) {

                events.add(this.buildEvent(GAUGE_METRIC_TYPE, METADATA_ERRORS,
                        1f, DEFAULT_SAMPLE_RATE, ImmutableMap.of("illegal_event", "illegal_event")));

                LOGGER.debug("Error building event on line: {}", line);
            }
        } catch (Exception e) {

            LOGGER.error(String.format("Error building event on line: %s", line), e);

            events.add(this.buildEvent(GAUGE_METRIC_TYPE, "invalid_lines",
                    1f, DEFAULT_SAMPLE_RATE, ImmutableMap.of("exception", e.getClass().getName())));
        }

        return events;
    }

    /**
     * validates if line received is valid.
     * throws InvalidStatsDLine if not.
     *
     * @param line - statsd line
     * @return statsd line metric elements, where [0] is metric name and [1] is metric metadata
     */
    private String[] validLine(final String line) {
        this.isNullOrEmpty(line);

        final String[] elements = line.split(":", 2);

        if (elements.length < 2 || elements[0].length() == 0) {
            throw new InvalidStatsDLineException(String.format("Bad line from StatsD: %s", line));
        }

        return elements;
    }

    /**
     * Validate metric metadata. Throws MalformedMetricComponentException if not valid.
     *
     * @param metadata - metric metadata, e.g: 320|ms|@0.1|#tag:tag_value,another_tag:tag_value
     * @return metadata components split by |
     */
    private String[] validLineMetadata(final String metadata) {
        this.isNullOrEmpty(metadata);

        String[] components = metadata.split("\\|");

        if (components.length < 2 || components.length > 4) {
            throw new MalformedMetricComponentException(String.format("Bad metric metadata: %s", metadata));
        }

        return components;
    }

    /**
     * parse dogstatsd metric tags into key-value pairs
     *
     * @param component - dogstatsd component line format
     * @return key-value pair of tags
     */
    private Map<String, String> parseDogStatsDTags(String component) {
        Objects.requireNonNull(component, "Component must not be null.");

        String[] tags = component.split(",");

        return Arrays.stream(tags)
                .map(tag -> {
                    final String[] kv = tag.replace("#", "").split(":", 2);

                    if (kv.length < 2 || kv[1].length() == 0) {
                        LOGGER.warn("Malformed or empty DogStatsD tag {} in component {}", tag, component);
                        return new String[]{};
                    }
                    return kv;
                })
                .filter(kv -> kv.length != 0)
                .collect(toMap(kv -> this.escapeString(kv[0]), kv -> this.escapeString(kv[1]))); //maybe we don't need to escape?
    }

    /**
     * escapes any non-valid character present in a metric name. metric names starting with numbers are invalid
     *
     * @param metricName - metric name
     * @return - escaped metric name
     */
    private String escapeString(String metricName) {
        Objects.requireNonNull(metricName, "Metric name must not be null.");

        // If a metric starts with a digit, prepend digit_.
        if (BEGINS_WITH_DIGITS.matcher(metricName).find()) {
            metricName = "digit_" + metricName;
        }

        // Replace all illegal metric chars with underscores.
        metricName = ILLEGAL_CHARACTERS.matcher(metricName).replaceAll("_");
        return metricName;
    }

    /**
     * Builds an instance of event
     *
     * @param metricType - type of the metric
     * @param metric     - metric name
     * @param value      - metric value
     * @param sampleRate - metric sample rate
     * @param tags       - metric tags
     * @return event instance
     */
    private DefaultEvent buildEvent(String metricType, String metric, float value, float sampleRate, Map<String, String> tags) {
        this.isNullOrEmpty(metric);
        this.isNullOrEmpty(metricType);

        try {
            return DefaultEvent.builder(metric)
                    .tags(tags)
                    .type(MetricType.fromString(metricType))
                    .value(value)
                    .sampleRate(sampleRate)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new UnknownMetricTypeException(String.format("Unknown metric type type %s", metricType));
        }
    }

    private void isNullOrEmpty(final String string) {
        Objects.requireNonNull(string);
        if (string.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s cannot be empty", string));
        }
    }
}
