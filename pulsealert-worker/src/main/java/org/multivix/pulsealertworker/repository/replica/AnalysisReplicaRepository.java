package org.multivix.pulsealertworker.repository.replica;

import org.multivix.pulsealertworker.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisReplicaRepository extends JpaRepository<Analysis, UUID> {
}