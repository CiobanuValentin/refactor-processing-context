// Ce se schimba in ratproxy in clasele care primeau (collectError, errorMessage, index, processingContext).
// Restul codului din aceste clase ramane neschimbat.

// --- LoanMapping ---------------------------------------------------------------------------------------

public static Loan map(Application application, Boolean isSanction, ProxyContext ctx) {
    // ... neschimbat, cu getApplicants(isSanction, applicationLoan, ctx)
}

private static List<Applicant> getApplicants(Boolean isSanction,
        com.bnp.pf.transformation.commons.api.message.input.Loan applicationLoan, ProxyContext ctx) {
    // ... neschimbat, cu ApplicantBuilder.createApplicant(customer, email, isSanction, ctx)
}

// Ca pana acum, un gen necunoscut nu opreste maparea: adauga o eroare si intoarce "".
public static String genderTransform(String input, ProxyContext ctx) {
    if (input != null) {
        return input.equals(GenderType.MALE.getGenderCode()) ? "M" : "V";
    }
    String message = "Error mapping data. Input is not of type GenderType.";
    log.error(message);
    ctx.addError(String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
    return StringUtils.EMPTY;
}

// --- datasource.model.Loan (la fel Applicant.validate si Company.validate) ------------------------------

// Nu mai primeste nimic: doar arunca. Eroarea o inregistreaza o singura data RatMappingService,
// in catch-ul din jurul apelului catre CloverLeaf (inainte se adauga aici si inca o data in catch).
public void validate() {
    if (this.totalAmountCurrency != null
            && !StringPatternUtils.patternMatches(this.totalAmountCurrency, "[A-Z]{3}$")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "TotalAmountCurrency doesn't match with the pattern [A-Z]{3}$");
    }
    if (this.applicants != null) {
        for (Applicant applicant : applicants) {
            if (applicant != null) {
                applicant.validate();
            }
        }
    }
    if (this.companies != null) {
        for (Company company : companies) {
            if (company != null) {
                company.validate();
            }
        }
    }
}

// --- BNPParibasCloverLeafAPI ------------------------------------------------------------------------------

Boolean putSanctionWithHttpMessages(String businessKey, Loan loan);
Boolean putComplianceWithHttpMessages(String businessKey, Loan loan);
Boolean putComplianceCompanyWithHttpMessages(String businessKey, Loan loan);
Risk getScoreWithHttpMessages(String businessKey);
