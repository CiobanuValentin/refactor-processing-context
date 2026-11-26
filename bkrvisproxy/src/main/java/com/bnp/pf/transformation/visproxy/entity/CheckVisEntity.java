package com.bnp.pf.transformation.visproxy.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Neschimbat. De verificat: FLOW_ID e unique, dar se salveaza un rand pentru fiecare client (vezi MIGRATION.md).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "CHECK_VIS_ENTITY")
public class CheckVisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "check_vis_entity_gen")
    @SequenceGenerator(name = "check_vis_entity_gen", sequenceName = "check_vis_entity_seq")
    private Integer id;

    @Column(name = "FLOW_ID", unique = true)
    private String flowId;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "BUSINESS_KEY")
    private String businessKey;

    @Column(name = "REQUEST_VIS_JSON", length = 100000)
    private String requestVisJson;

    @Column(name = "RESPONSE_VIS_JSON", length = 100000)
    private String responseVisJson;

    @Column(name = "ERROR_HANDLER_JSON", length = 100000)
    private String errorHandlerJson;
}
