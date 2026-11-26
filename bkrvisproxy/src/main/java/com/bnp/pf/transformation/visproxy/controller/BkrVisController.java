package com.bnp.pf.transformation.visproxy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.visproxy.service.VisCheckRecordService;

/**
 * Controller used in RAT proxy for fetching data
 */
@RestController
@RequiredArgsConstructor
public class BkrVisController {
    private final VisCheckRecordService visCheckRecordService;

    @GetMapping("/getVisEntities")
    public List<GeneralProxyObsDto> getVisEntities(String businessKey, String flowId) {
        return visCheckRecordService.fetchVisEntity(businessKey, flowId);
    }
}
