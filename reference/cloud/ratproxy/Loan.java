// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.datasource.model;

@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Data
@Builder
public class Loan implements Serializable {

    // Possible values include: 'ok', 'scal_unavailable'
    @JsonProperty("scal_availability")
    private String scalAvailability;
    @JsonProperty("supplier")
    private String supplier;
    @JsonProperty("product_type")
    private String productType;
    @JsonProperty("amount")
    private Integer amount;
    // Loan duration in months
    @JsonProperty("duration")
    private Integer duration;
    @JsonProperty("sales_channel")
    private String salesChannel;
    @JsonProperty("dealer_coc")
    private String dealerCoc;
    @JsonProperty("sales_channel_subbroker")
    @Builder.Default
    private String salesChannelSubbroker = StringUtils.EMPTY;
    // ISO 4217 Currency code
    @NotNull
    @JsonProperty("total_amount_currency")
    private String totalAmountCurrency;
    // $applicants
    @JsonProperty("applicants")
    private List<Applicant> applicants;
    // $companies
    @JsonProperty("companies")
    private List<Company> companies;


    // Validate the object. Throws ValidationException if validation fails.

    public void validate(CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        if (this.totalAmountCurrency != null && !StringPatternUtils.patternMatches(this.totalAmountCurrency, "[A-Z]{3}$")) {
            String message = "TotalAmountCurrency doesn't match with the pattern [A-Z]{3}$";
            log.error(message);
            collectError.addError(processingContext, String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
            processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, errorMessage);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        if (this.applicants != null) {
            for (Applicant applicant : applicants) {

                if (applicant != null) {
                    applicant.validate(collectError, index, errorMessage, processingContext);
                }
            }
        }
        if (this.companies != null) {
            for (Company company : companies) {
                if (company != null) {
                    company.validate(collectError, index, errorMessage, processingContext);
                }
            }
        }
        /* ...restul metodei nu era in poza */
    }
}
