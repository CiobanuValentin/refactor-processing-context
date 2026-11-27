// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.repository;

@Repository
public interface CreditCheckRepository extends JpaRepository<CreditCheckEntity, String> {
    Optional<CreditCheckEntity> findTopByBusinessKeyAndFlowIdOrderByCreatedDateDesc(String businessKey, String flowId);
}
