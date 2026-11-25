package com.bnpp.pf.transformation.ratproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ProxyResultsDto;
// TODO: pachetul lui BNPParibasCloverLeafAPI este presupus
import com.bnpp.pf.transformation.ratproxy.client.BNPParibasCloverLeafAPI;
import com.bnpp.pf.transformation.ratproxy.service.RatResultsService;

@RestController
@RequiredArgsConstructor
public class RatController {

    private final BNPParibasCloverLeafAPI bnpParibasCloverleafAPI;
    private final RatResultsService ratResultsService;

    @GetMapping("/ping")
    public String ping() {
        return bnpParibasCloverleafAPI.ping();
    }

    /** Folosit de sed: risk assessment-urile "rat.compliance", cate unul per client sau companie. */
    @GetMapping("/ratCompliance/results")
    public ProxyResultsDto getRatComplianceResults(@RequestParam String businessKey, @RequestParam String flowId) {
        return ratResultsService.results(businessKey, flowId);
    }
}
