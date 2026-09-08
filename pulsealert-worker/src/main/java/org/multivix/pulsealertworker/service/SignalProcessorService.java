package org.multivix.pulsealertworker.service;

import com.rabbitmq.client.Channel;
import org.multivix.pulsealertworker.dto.ProcessedSignalEvent;
import org.multivix.pulsealertworker.dto.VitalSignEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class SignalProcessorService {

    private static final Logger log = LoggerFactory.getLogger(SignalProcessorService.class);
    public static final String QUEUE_PROCESSED_EVENTS = "processed_events";

    private final AtomicLong lamportClock = new AtomicLong(0);
    private final RabbitTemplate rabbitTemplate;

    @Value("${worker.id:1}")
    private int workerId;

    public SignalProcessorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "vital_signals")
    public void processVitalSignal(VitalSignEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // R4: Sincronização Lógica
            long currentClock = lamportClock.updateAndGet(current -> Math.max(current, event.lamportClock()) + 1);

            log.info("[WORKER-{}] Evento {} recebido. Lamport atualizado para: {}", workerId, event.eventId(), currentClock);
            log.info("[WORKER-{}] Processando FC: {}, SpO2: {}, PA Sistólica: {}",
                    workerId, event.heartRate(), event.spo2(), event.systolicPressure());

            // Processamento Clínico
            boolean isArrhythmia = detectArrhythmia(event.heartRate());
            boolean isShock = detectShock(event.systolicPressure(), event.spo2());

            log.info("[WORKER-{}] Diagnóstico -> Arritmia: {} | Choque: {}", workerId, isArrhythmia, isShock);

            // Monta o evento processado enviando o sinal original completo
            ProcessedSignalEvent processedEvent = new ProcessedSignalEvent(
                    event,
                    isArrhythmia,
                    isShock,
                    currentClock,
                    workerId
            );

            // Publica o resultado para consolidação do Líder
            rabbitTemplate.convertAndSend(QUEUE_PROCESSED_EVENTS, processedEvent);

            // Confirmação explícita de sucesso
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[WORKER-{}] Falha ao processar evento {}. Devolvendo para a fila.", workerId, event.eventId(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackException) {
                log.error("[WORKER-{}] Erro crítico no NACK", workerId, nackException);
            }
        }
    }

    private boolean detectArrhythmia(double heartRate) {
        return heartRate > 140.0 || heartRate < 40.0;
    }

    private boolean detectShock(double systolicPressure, double spo2) {
        return systolicPressure < 90.0 && spo2 < 90.0;
    }
}