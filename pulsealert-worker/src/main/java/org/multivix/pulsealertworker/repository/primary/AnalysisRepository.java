package org.multivix.pulsealertworker.repository.primary;

import org.multivix.pulsealertworker.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
}