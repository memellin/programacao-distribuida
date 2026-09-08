package org.multivix.pulsealertworker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vital_signs")
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "bed_id", nullable = false)
    private String bedId;

    @Column(name = "heart_rate", nullable = false)
    private double heartRate;

    @Column(name = "spo2", nullable = false)
    private double spo2;

    @Column(name = "systolic_pressure", nullable = false)
    private double systolicPressure;

    @Column(name = "diastolic_pressure", nullable = false)
    private double diastolicPressure;

    @Column(name = "temperature", nullable = false)
    private double temperature;

    @Column(name = "lamport_clock", nullable = false)
    private long lamportClock;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getBedId() { return bedId; }
    public void setBedId(String bedId) { this.bedId = bedId; }

    public double getHeartRate() { return heartRate; }
    public void setHeartRate(double heartRate) { this.heartRate = heartRate; }

    public double getSpo2() { return spo2; }
    public void setSpo2(double spo2) { this.spo2 = spo2; }

    public double getSystolicPressure() { return systolicPressure; }
    public void setSystolicPressure(double systolicPressure) { this.systolicPressure = systolicPressure; }

    public double getDiastolicPressure() { return diastolicPressure; }
    public void setDiastolicPressure(double diastolicPressure) { this.diastolicPressure = diastolicPressure; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public long getLamportClock() { return lamportClock; }
    public void setLamportClock(long lamportClock) { this.lamportClock = lamportClock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}