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
public class UdpConfiguration {
    @JsonProperty
    private Integer port;
    @JsonProperty
    private String host;
}
