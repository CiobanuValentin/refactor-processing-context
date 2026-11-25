package com.bnpp.pf.transformation.ratproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.bnpp.pf.transformation.ratproxy.datasource.BNPParibasCloverLeafAPI;

/** Neschimbat fata de varianta actuala. */
@RestController
@RequiredArgsConstructor
public class RatController {

    private final BNPParibasCloverLeafAPI bnpParibasCloverleafAPI;

    @GetMapping("/ping")
    public String ping() {
        return bnpParibasCloverleafAPI.ping();
    }
}
