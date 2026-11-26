package com.bnp.pf.transformation.visproxy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bnp.pf.transformation.visproxy.entity.CheckVisEntity;

// Neschimbat.
@Repository
public interface CheckVisEntityRepository extends JpaRepository<CheckVisEntity, String> {
    List<CheckVisEntity> findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(String businessKey, String flowId);
}
