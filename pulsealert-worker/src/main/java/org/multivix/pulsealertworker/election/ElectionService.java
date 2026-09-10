package org.multivix.pulsealertworker.election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ElectionService {

    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    private final WorkerState workerState;
    private final RestTemplate restTemplate;

    public ElectionService(WorkerState workerState) {
        this.workerState = workerState;
        this.restTemplate = new RestTemplate();
    }

    // Executa este bloco a cada 5000 milissegundos (5 segundos)
    @Scheduled(fixedDelay = 5000)
    public void checkLeaderHealth() {
        if (workerState.isLeader()) {
            return; // O líder atual não precisa fazer o ping em si mesmo
        }

        int leaderId = workerState.getCurrentLeaderId();

        if (leaderId == -1) {
            log.info("[MONITOR] A rede está sem líder. Iniciando votação...");
            startElection();
            return;
        }

        String leaderUrl = resolveWorkerUrl(leaderId);

        try {
            // Tenta atingir o endpoint /heartbeat do líder
            restTemplate.getForEntity(leaderUrl + "/api/ring/heartbeat", String.class);
            // Se passar dessa linha, o líder respondeu 200 OK
        } catch (Exception e) {
            log.warn("[MONITOR] Falha de comunicação com o Líder {}! Destituindo líder e iniciando nova eleição...", leaderId);
            workerState.setLeader(-1); // Remove o líder inoperante da memória
            startElection();
        }
    }

    public void startElection() {
        try {
            String nextUrl = workerState.getNextWorkerUrl() + "/api/ring/election";
            Map<String, Integer> payload = Map.of(
                    "initiatorId", workerState.getWorkerId(),
                    "highestId", workerState.getWorkerId()
            );

            restTemplate.postForEntity(nextUrl, payload, Void.class);
            log.info("[ELEIÇÃO] Voto repassado para o vizinho no anel: {}", nextUrl);
        } catch (Exception e) {
            log.error("[ELEIÇÃO] Vizinho inoperante ou erro de rede ao repassar eleição: {}", e.getMessage());
        }
    }

    private String resolveWorkerUrl(int id) {
        // Usa o nome do serviço Docker para comunicação entre contêineres na rede pulse_net
        return "http://worker" + id + ":8080";
    }
}