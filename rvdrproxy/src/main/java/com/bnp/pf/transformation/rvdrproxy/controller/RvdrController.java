package com.bnp.pf.transformation.rvdrproxy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.rvdrproxy.service.CreditCheckRecordService;

/**
 * Controller used in RAT proxy for fetching data
 */
@RestController
@RequiredArgsConstructor
public class RvdrController {
    private final CreditCheckRecordService creditCheckRecordService;

    /**
     * Intoarce toate randurile (unul pentru fiecare client verificat), ca in legacy; inainte intorcea doar ultimul.
     * Calea ramane /checkCredit, deci rvdr.checkCredit-endpoint din agregatoare nu se schimba.
     */
    @GetMapping("/checkCredit")
    public List<GeneralProxyObsDto> getCreditChecks(@RequestParam String businessKey, @RequestParam String flowId) {
        return creditCheckRecordService.fetchCheckCredits(businessKey, flowId);
    }
}
