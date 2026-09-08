package org.multivix.pulsealertworker.repository.primary;

import org.multivix.pulsealertworker.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}