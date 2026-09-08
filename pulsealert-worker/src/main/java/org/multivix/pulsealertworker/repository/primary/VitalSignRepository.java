package org.multivix.pulsealertworker.repository.primary;

import org.multivix.pulsealertworker.model.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VitalSignRepository extends JpaRepository<VitalSign, UUID> {
    // Busca customizada caso o Líder precise verificar a existência de um evento
    boolean existsByEventId(String eventId);
}