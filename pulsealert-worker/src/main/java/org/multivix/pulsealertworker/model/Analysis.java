package org.multivix.pulsealertworker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vital_sign_id", nullable = false)
    private VitalSign vitalSign;

    @Column(name = "worker_id", nullable = false)
    private int workerId;

    @Column(name = "arrhythmia_detected", nullable = false)
    private boolean arrhythmiaDetected;

    @Column(name = "shock_detected", nullable = false)
    private boolean shockDetected;

    @Column(name = "lamport_clock", nullable = false)
    private long lamportClock;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VitalSign getVitalSign() {
        return vitalSign;
    }

    public void setVitalSign(VitalSign vitalSign) {
        this.vitalSign = vitalSign;
    }

    public int getWorkerId() {
        return workerId;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public boolean isArrhythmiaDetected() {
        return arrhythmiaDetected;
    }

    public void setArrhythmiaDetected(boolean arrhythmiaDetected) {
        this.arrhythmiaDetected = arrhythmiaDetected;
    }

    public boolean isShockDetected() {
        return shockDetected;
    }

    public void setShockDetected(boolean shockDetected) {
        this.shockDetected = shockDetected;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lamportClock = lamportClock;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}