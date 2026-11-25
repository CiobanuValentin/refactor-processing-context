// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.controller;

@RestController
@RequiredArgsConstructor
public class RatController {

    private final BNPParibasCloverLeafAPI bnpParibasCloverleafAPI;

    @GetMapping("/ping")
    public String ping() { return bnpParibasCloverleafAPI.ping(); }
}
