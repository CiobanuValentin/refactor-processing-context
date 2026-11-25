package com.bnpp.pf.transformation.ratproxy.datasource;

import com.bnpp.pf.transformation.ratproxy.datasource.model.Loan;
import com.bnpp.pf.transformation.ratproxy.datasource.model.Risk;

/**
 * Apelurile catre CloverLeaf. Metodele nu mai primesc collectError / index / errorMessage / context:
 * la eroare arunca, iar RatMappingService inregistreaza eroarea in context, o singura data.
 */
public interface BNPParibasCloverLeafAPI {

    Risk getScoreWithHttpMessages(String applicationId);

    Boolean putSanctionWithHttpMessages(String applicationId, Loan loan);

    Boolean putComplianceCompanyWithHttpMessages(String applicationId, Loan loan);

    Boolean putComplianceWithHttpMessages(String applicationId, Loan loan);

    String ping();
}
