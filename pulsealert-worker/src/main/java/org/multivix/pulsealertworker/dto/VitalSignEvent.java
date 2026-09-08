package org.multivix.pulsealertworker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VitalSignEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("bedId") String bedId,
        @JsonProperty("heartRate") double heartRate,
        @JsonProperty("spo2") double spo2,
        @JsonProperty("systolicPressure") double systolicPressure,
        @JsonProperty("diastolicPressure") double diastolicPressure,
        @JsonProperty("temperature") double temperature,
        @JsonProperty("lamportClock") long lamportClock
) {}