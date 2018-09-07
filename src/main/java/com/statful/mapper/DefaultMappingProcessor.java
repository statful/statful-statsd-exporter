package com.statful.mapper;

import com.statful.config.Mapping;
import com.statful.domain.DefaultEvent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultMappingProcessor implements MappingProcessor<DefaultEvent> {

    private List<Mapping> mappings;

    public DefaultMappingProcessor(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public DefaultEvent process(DefaultEvent e) {
        return mappings
                .stream()
                .filter(m -> this.matches(e.getMetricName(), m.getMatch()))
                .findFirst()
                .map(m -> DefaultEvent.builder(m.getName())
                        .type(e.getType())
                        .sampleRate(e.getSampleRate())
                        .value(e.getValue())
                        .tags(tags(e.getMetricName(), m))
                        .build())
                .orElse(e);
    }

    @Override
    public boolean processable(String metric) {
        return mappings
                .stream()
                .filter(m -> this.matches(metric, m.getMatch()))
                .findFirst()
                .map(m -> m.getAction().equals(Mapping.Action.MATCH))
                .orElse(true);
    }

    /**
     * extract tags given a specific mapping and a corresponding metric.
     * <p>
     * e.g:
     * mapping match -> test.dispatcher.*.*.*
     * mapping tags -> label:$1, label2: $2, label3: $3
     * metric -> test.dispatcher.FooProcessor.send.success
     * <p>
     * resulting tags -> label:FooProcessor, label2:send, label3: success
     *
     * @param metric - metric name
     * @param m      - mapping
     * @return tags extracted
     */
    private Map<String, String> tags(final String metric, final Mapping m) {
        Objects.requireNonNull(m);
        Objects.requireNonNull(m.getMatch());
        Objects.requireNonNull(metric);

        Map<String, String> tags = new HashMap<>();
        String[] metricData = metric.split("\\.");

        List<Map.Entry<String, String>> list = Optional.ofNullable(m.getTags())
                .map(x -> m.getTags().entrySet()
                        .stream()
                        .filter(es -> es.getValue().contains("$") || es.getKey().contains("$"))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());


        for (final Map.Entry<String, String> tag : list) {

            String key = tag.getKey();
            String value = tag.getValue();

            if (key.contains("$")) {
                final Integer index = Integer.valueOf(key.replace("$", ""));
                final String dataValue = metricData[index - 1];
                key = key.replaceFirst("\\$.", dataValue);
            }

            if (value.contains("$")) {
                final Integer index = Integer.valueOf(value.replace("$", ""));
                final String dataValue = metricData[index - 1];
                value = value.replaceFirst("\\$.", dataValue);
            }

            tags.put(key, value);
        }

        return tags;
    }


    private boolean matches(final String metric, final String globedMatch) {
        Objects.requireNonNull(metric);
        Objects.requireNonNull(globedMatch);

        return Pattern.compile(
                globedMatch
                        .replace(".", "\\.")
                        .replace("*", "[a-zA-Z_]+")
        ).matcher(metric).matches();
    }
}
