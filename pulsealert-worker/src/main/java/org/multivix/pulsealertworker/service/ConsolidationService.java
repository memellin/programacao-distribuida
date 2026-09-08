package org.multivix.pulsealertworker.service;

import com.rabbitmq.client.Channel;
import org.multivix.pulsealertworker.config.RabbitMQConfig;
import org.multivix.pulsealertworker.dto.ProcessedSignalEvent;
import org.multivix.pulsealertworker.model.Alert;
import org.multivix.pulsealertworker.model.Analysis;
import org.multivix.pulsealertworker.model.VitalSign;
import org.multivix.pulsealertworker.repository.primary.AlertRepository;
import org.multivix.pulsealertworker.repository.primary.AnalysisRepository;
import org.multivix.pulsealertworker.repository.primary.VitalSignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.multivix.pulsealertworker.repository.replica.AlertReplicaRepository;
import org.multivix.pulsealertworker.repository.replica.AnalysisReplicaRepository;
import org.multivix.pulsealertworker.repository.replica.VitalSignReplicaRepository;

import java.time.LocalDateTime;

@Service
public class ConsolidationService {

    private static final Logger log = LoggerFactory.getLogger(ConsolidationService.class);

    private final VitalSignRepository vitalSignRepo;
    private final AnalysisRepository analysisRepo;
    private final AlertRepository alertRepo;

    private final VitalSignReplicaRepository vitalSignReplicaRepo;
    private final AnalysisReplicaRepository analysisReplicaRepo;
    private final AlertReplicaRepository alertReplicaRepo;

    private final TransactionTemplate primaryTxTemplate;
    private final TransactionTemplate replicaTxTemplate;

    public ConsolidationService(VitalSignRepository vitalSignRepo, AnalysisRepository analysisRepo, AlertRepository alertRepo,
                                VitalSignReplicaRepository vitalSignReplicaRepo, AnalysisReplicaRepository analysisReplicaRepo, AlertReplicaRepository alertReplicaRepo,
                                @Qualifier("primaryTransactionManager") PlatformTransactionManager primaryTxManager,
                                @Qualifier("replicaTransactionManager") PlatformTransactionManager replicaTxManager) {
        this.vitalSignRepo = vitalSignRepo;
        this.analysisRepo = analysisRepo;
        this.alertRepo = alertRepo;
        this.vitalSignReplicaRepo = vitalSignReplicaRepo;
        this.analysisReplicaRepo = analysisReplicaRepo;
        this.alertReplicaRepo = alertReplicaRepo;

        this.primaryTxTemplate = new TransactionTemplate(primaryTxManager);
        this.replicaTxTemplate = new TransactionTemplate(replicaTxManager);
    }

    @RabbitListener(id = "consolidationListener", queues = RabbitMQConfig.QUEUE_PROCESSED_EVENTS, autoStartup = "false")
    public void consolidateData(ProcessedSignalEvent processedEvent, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String eventId = processedEvent.originalSignal().eventId();

            log.info("[LÍDER] Consolidando evento {} processado pelo Worker {}", eventId, processedEvent.workerId());

            boolean existsInPrimary = vitalSignRepo.existsByEventId(eventId);
            boolean existsInReplica = vitalSignReplicaRepo.existsByEventId(eventId);

            if (existsInPrimary || existsInReplica) {
                log.warn("[LÍDER] Evento {} já existente na base de dados (Recuperação de Falha). Ignorando duplicata para evitar loop.", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 1. Gravação transacional no Banco Primário
            primaryTxTemplate.executeWithoutResult(status -> {
                VitalSign primaryVital = mapVitalSign(processedEvent);
                Analysis primaryAnalysis = mapAnalysis(processedEvent, primaryVital);
                analysisRepo.save(primaryAnalysis);

                if (processedEvent.arrhythmiaDetected() || processedEvent.shockDetected()) {
                    Alert primaryAlert = mapAlert(processedEvent, primaryAnalysis);
                    alertRepo.save(primaryAlert);
                }
            });

            // 2. Gravação transacional no Banco de Réplica
            replicaTxTemplate.executeWithoutResult(status -> {
                VitalSign replicaVital = mapVitalSign(processedEvent);
                Analysis replicaAnalysis = mapAnalysis(processedEvent, replicaVital);
                analysisReplicaRepo.save(replicaAnalysis);

                if (processedEvent.arrhythmiaDetected() || processedEvent.shockDetected()) {
                    Alert replicaAlert = mapAlert(processedEvent, replicaAnalysis);
                    alertReplicaRepo.save(replicaAlert);
                    log.warn("[LÍDER] ALERTA CRÍTICO GERADO E REPLICADO PARA O LEITO: {}", processedEvent.originalSignal().bedId());
                }
            });

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[LÍDER] Erro ao gravar dados no banco. Devolvendo para a fila.", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackException) {
                log.error("[LÍDER] Falha crítica no NACK", nackException);
            }
        }
    }

    private VitalSign mapVitalSign(ProcessedSignalEvent event) {
        VitalSign vs = new VitalSign();
        vs.setEventId(event.originalSignal().eventId());
        vs.setCorrelationId(event.originalSignal().correlationId());
        vs.setBedId(event.originalSignal().bedId());
        vs.setHeartRate(event.originalSignal().heartRate());
        vs.setSpo2(event.originalSignal().spo2());
        vs.setSystolicPressure(event.originalSignal().systolicPressure());
        vs.setDiastolicPressure(event.originalSignal().diastolicPressure());
        vs.setTemperature(event.originalSignal().temperature());
        vs.setLamportClock(event.originalSignal().lamportClock());
        vs.setCreatedAt(LocalDateTime.now());
        return vs;
    }

    private Analysis mapAnalysis(ProcessedSignalEvent event, VitalSign vs) {
        Analysis a = new Analysis();
        a.setVitalSign(vs);
        a.setWorkerId(event.workerId());
        a.setArrhythmiaDetected(event.arrhythmiaDetected());
        a.setShockDetected(event.shockDetected());
        a.setLamportClock(event.lamportClock());
        a.setProcessedAt(LocalDateTime.now());
        return a;
    }

    private Alert mapAlert(ProcessedSignalEvent event, Analysis a) {
        Alert al = new Alert();
        al.setAnalysis(a);
        al.setType(event.shockDetected() ? "CHOQUE HIPOVOLÊMICO" : "ARRITMIA SEVERA");
        al.setSeverity("CRÍTICA");
        al.setStatus("NÃO ATENDIDO");
        al.setCreatedAt(LocalDateTime.now());
        return al;
    }
}