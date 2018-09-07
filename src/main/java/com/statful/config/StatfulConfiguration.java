package com.statful.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class StatfulConfiguration {
    @JsonProperty
    private Long flushInterval;
    @JsonProperty
    private Integer flushSize;
    @JsonProperty
    private Boolean dryRun;
    @JsonProperty
    private String host;
    @JsonProperty
    private Integer port;
    @JsonProperty
    private String app;
    @JsonProperty
    private String namespace;
    @JsonProperty
    private String environment;
}
