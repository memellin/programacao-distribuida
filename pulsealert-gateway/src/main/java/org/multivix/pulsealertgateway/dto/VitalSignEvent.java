package org.multivix.pulsealertgateway.dto;

public record VitalSignEvent(
        String eventId,
        String correlationId,
        String bedId,
        double heartRate,
        double spo2,
        double systolicPressure,
        double diastolicPressure,
        double temperature,
        long lamportClock
) {}