package org.multivix.pulsealertworker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcessedSignalEvent(
        @JsonProperty("originalSignal") VitalSignEvent originalSignal,
        @JsonProperty("arrhythmiaDetected") boolean arrhythmiaDetected,
        @JsonProperty("shockDetected") boolean shockDetected,
        @JsonProperty("lamportClock") long lamportClock,
        @JsonProperty("workerId") int workerId
) {}