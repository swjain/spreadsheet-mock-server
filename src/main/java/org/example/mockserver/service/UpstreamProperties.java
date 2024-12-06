package org.example.mockserver.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties
public class UpstreamProperties {

    private Map<String, String> upstreams;

    public Map<String, String> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(Map<String, String> upstreams) {
        this.upstreams = upstreams;
    }
}