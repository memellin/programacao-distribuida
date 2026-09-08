package org.multivix.pulsealertworker.repository.replica;

import org.multivix.pulsealertworker.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertReplicaRepository extends JpaRepository<Alert, UUID> {

}