package com.placement.portal.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Batch.BatchId> {
    List<Batch> findByProgramId(String programId);
}
