package com.yaoshizuting.controller;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitoringControllerTest {

    @Test
    void prometheusReturnsUnavailableMessageWhenRegistryMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PrometheusMeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        MonitoringController controller = new MonitoringController(provider);

        assertEquals("# Prometheus registry unavailable\n", controller.prometheus());
    }

    @Test
    void prometheusReturnsRegistryScrape() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PrometheusMeterRegistry> provider = mock(ObjectProvider.class);
        PrometheusMeterRegistry registry = mock(PrometheusMeterRegistry.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        when(registry.scrape()).thenReturn("metric_total 1.0\n");
        MonitoringController controller = new MonitoringController(provider);

        assertEquals("metric_total 1.0\n", controller.prometheus());
    }
}
