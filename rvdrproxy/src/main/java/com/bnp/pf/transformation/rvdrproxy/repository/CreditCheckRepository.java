package com.bnp.pf.transformation.rvdrproxy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bnp.pf.transformation.rvdrproxy.entity.CreditCheckEntity;

@Repository
public interface CreditCheckRepository extends JpaRepository<CreditCheckEntity, String> {
    /** Toate randurile (unul pe client verificat); inlocuieste findTopByBusinessKeyAndFlowIdOrderByCreatedDateDesc. */
    List<CreditCheckEntity> findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(String businessKey, String flowId);
}
