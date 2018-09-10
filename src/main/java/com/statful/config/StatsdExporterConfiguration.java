package com.statful.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.statful.client.core.udp.StatfulFactory;
import com.statful.client.domain.api.StatfulClient;
import com.statful.client.domain.api.Transport;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.statful.util.Constants.ENVIRONMENT;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class StatsdExporterConfiguration {
    @JsonProperty("tcp")
    private TcpConfiguration tcpConfiguration;
    @JsonProperty("udp")
    private UdpConfiguration udpConfiguration;
    @JsonProperty("mappings")
    private List<Mapping> mappingsList;
    @JsonProperty("statful")
    private StatfulConfiguration statfulConfiguration;
    @JsonProperty
    private String environment;

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static StatsdExporterConfiguration loadConfiguration(final String path) throws IOException {
        StatsdExporterConfiguration statsdExporterConfiguration = YAML_MAPPER
                .readValue(Paths.get(path).toFile(), StatsdExporterConfiguration.class);

        statsdExporterConfiguration.getMappingsList()
                .forEach(m -> {
                    m.setAction(Optional.ofNullable(m.getAction()).orElse(Mapping.Action.MATCH));

                    if (Mapping.Action.MATCH.equals(m.getAction())) {
                        Objects.requireNonNull(m.getName());
                    }

                    Objects.requireNonNull(m.getMatch());
                });

        return statsdExporterConfiguration;
    }

    public StatfulClient getStatfulClient() {
        return StatfulFactory
                .buildUDPClient() //see if I can replace this with a nonBlocking udp client. (udp client will only block when socket buffer is full
                .with()
                .isDryRun(statfulConfiguration.getDryRun())
                .host(statfulConfiguration.getHost())
                .port(statfulConfiguration.getPort())
                .namespace(statfulConfiguration.getNamespace())
                .app(statfulConfiguration.getApp())
                .flushInterval(statfulConfiguration.getFlushInterval())
                .flushSize(statfulConfiguration.getFlushSize())
                .tag(ENVIRONMENT, environment)
                .transport(Transport.UDP)
                .build();
    }
}
