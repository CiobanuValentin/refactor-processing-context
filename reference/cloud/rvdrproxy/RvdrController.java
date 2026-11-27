// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.controller;

/**
 * Controller used in RAT proxy for fetching data
 */
@RestController
@RequiredArgsConstructor
public class RvdrController {
    private final CreditCheckRecordService creditCheckRecordService;

    @GetMapping("/checkCredit")
    public GeneralProxyObsDto getCreditCheck(@RequestParam String businessKey, @RequestParam String flowId) {
        return creditCheckRecordService.fetchCheckCredit(businessKey, flowId);
    }
}
