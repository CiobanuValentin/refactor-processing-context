// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.model;

@Data
@RequiredArgsConstructor
public class CreditCheckResult {
    private final CreditCheckResponse response;
    private final List<Error> errors;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
