package org.multivix.pulsealertworker.election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ring")
public class ElectionController {

    private static final Logger log = LoggerFactory.getLogger(ElectionController.class);

    private final WorkerState workerState;
    private final RestTemplate restTemplate;

    public ElectionController(WorkerState workerState) {
        this.workerState = workerState;
        this.restTemplate = new RestTemplate();
    }

    // Endpoint 1: Recebe a mensagem de eleição do Worker anterior no anel
    @PostMapping("/election")
    public ResponseEntity<Void> processElection(@RequestBody Map<String, Integer> payload) {
        int initiatorId = payload.get("initiatorId");
        int highestId = payload.get("highestId");

        log.info("[ELEIÇÃO] Recebida eleição iniciada por {}, maior ID até agora: {}", initiatorId, highestId);

        if (initiatorId == workerState.getWorkerId()) {
            // A mensagem deu a volta completa no anel. O maior ID vence.
            workerState.setLeader(highestId);
            announceLeader(highestId, workerState.getWorkerId());
        } else {
            // Repassa a eleição para o próximo vizinho, atualizando o maior ID se o meu for maior
            int newHighestId = Math.max(highestId, workerState.getWorkerId());
            forwardElection(initiatorId, newHighestId);
        }

        return ResponseEntity.ok().build();
    }

    // Endpoint 2: Recebe o anúncio de quem venceu a eleição
    @PostMapping("/coordinator")
    public ResponseEntity<Void> processCoordinator(@RequestBody Map<String, Integer> payload) {
        int newLeaderId = payload.get("leaderId");
        int initiatorId = payload.get("initiatorId");

        if (initiatorId != workerState.getWorkerId()) {
            workerState.setLeader(newLeaderId);
            // Continua propagando o resultado até dar a volta
            announceLeader(newLeaderId, initiatorId);
        }

        return ResponseEntity.ok().build();
    }

    // Endpoint 3: Heartbeat (Usado para saber se o líder está vivo)
    @GetMapping("/heartbeat")
    public ResponseEntity<String> heartbeat() {
        return ResponseEntity.ok("ALIVE");
    }

    private void forwardElection(int initiatorId, int highestId) {
        try {
            String url = workerState.getNextWorkerUrl() + "/api/ring/election";
            restTemplate.postForEntity(url, Map.of("initiatorId", initiatorId, "highestId", highestId), Void.class);
        } catch (Exception e) {
            log.error("[ELEIÇÃO] Falha ao contatar vizinho: {}", e.getMessage());
        }
    }

    private void announceLeader(int leaderId, int initiatorId) {
        try {
            String url = workerState.getNextWorkerUrl() + "/api/ring/coordinator";
            restTemplate.postForEntity(url, Map.of("leaderId", leaderId, "initiatorId", initiatorId), Void.class);
        } catch (Exception e) {
            log.error("[ELEIÇÃO] Falha ao anunciar líder ao vizinho: {}", e.getMessage());
        }
    }
}