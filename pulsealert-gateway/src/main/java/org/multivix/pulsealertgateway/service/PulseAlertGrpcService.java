package org.multivix.pulsealertgateway.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.multivix.pulsealertgateway.config.RabbitMQConfig;
import org.multivix.pulsealertgateway.dto.VitalSignEvent;
import org.multivix.pulsealert.grpc.PulseAlertServiceGrpc;
import org.multivix.pulsealert.grpc.VitalSignRequest;
import org.multivix.pulsealert.grpc.VitalSignResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@GrpcService
public class PulseAlertGrpcService extends PulseAlertServiceGrpc.PulseAlertServiceImplBase {

    private final RabbitTemplate rabbitTemplate;

    public PulseAlertGrpcService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendVitalSign(VitalSignRequest request, StreamObserver<VitalSignResponse> responseObserver) {

        // 1. Mapeia a requisição gRPC para o evento do RabbitMQ (inicializando o Lamport em 0)
        VitalSignEvent event = new VitalSignEvent(
                request.getEventId(),
                request.getCorrelationId(),
                request.getBedId(),
                request.getHeartRate(),
                request.getSpo2(),
                request.getSystolicPressure(),
                request.getDiastolicPressure(),
                request.getTemperature(),
                0L
        );

        // 2. Publica o evento de forma assíncrona na mensageria
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_VITAL_SIGNALS, event);

        // 3. Monta a resposta imediata confirmando o recebimento (ACK)
        VitalSignResponse response = VitalSignResponse.newBuilder()
                .setEventId(request.getEventId())
                .setStatus("RECEIVED")
                .setMessage("Evento alocado na fila de processamento")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}