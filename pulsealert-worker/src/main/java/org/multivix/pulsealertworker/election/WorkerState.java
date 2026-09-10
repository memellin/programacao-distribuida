package org.multivix.pulsealertworker.election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerState {

    private static final Logger log = LoggerFactory.getLogger(WorkerState.class);

    private final RabbitListenerEndpointRegistry registry;

    @Value("${worker.id:1}")
    private int workerId;

    @Value("${worker.next.url:http://localhost:8081}")
    private String nextWorkerUrl;

    private volatile boolean isLeader = false;
    private volatile int currentLeaderId = -1;

    public WorkerState(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public int getWorkerId() {
        return workerId;
    }

    public String getNextWorkerUrl() {
        return nextWorkerUrl;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public int getCurrentLeaderId() {
        return currentLeaderId;
    }

    public void setLeader(int leaderId) {
        this.currentLeaderId = leaderId;
        boolean wasLeader = this.isLeader;
        this.isLeader = (this.workerId == leaderId);

        var container = registry.getListenerContainer("consolidationListener");

        if (this.isLeader) {
            log.info("[ELEIÇÃO] Eu sou o novo Líder! (Worker {}).", workerId);
            if (container != null && !container.isRunning()) {
                log.info("[ELEIÇÃO] Iniciando serviço de gravação.");
                container.start();
            }
        } else {
            log.info("[ELEIÇÃO] O novo Líder é o Worker {}", leaderId);
            if (wasLeader && container != null && container.isRunning()) {
                log.info("[ELEIÇÃO] Fui rebaixado. Parando serviço de gravação.");
                container.stop();
            }
        }
    }
}