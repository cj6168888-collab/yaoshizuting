package com.yaoshizuting.controller;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringController {

    private static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final ObjectProvider<PrometheusMeterRegistry> prometheusMeterRegistry;

    public MonitoringController(ObjectProvider<PrometheusMeterRegistry> prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @GetMapping(value = "/actuator/prometheus", produces = PROMETHEUS_CONTENT_TYPE)
    public String prometheus() {
        PrometheusMeterRegistry registry = prometheusMeterRegistry.getIfAvailable();
        return registry == null ? "# Prometheus registry unavailable\n" : registry.scrape();
    }
}
