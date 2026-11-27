// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCheckRecordService {
    private final CreditCheckRepository creditCheckRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;

    public void save(ProcessingContext processingContext, CreditCheckRequest creditCheckRequest,
                     String responseJson, List<Error> errors, LocalDate checkDate) {
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setErrors(new ArrayList<>(errors));

        GeneralProxyObsDto creditCheckDto = Optional.ofNullable(
                BuildDTOsUtility.buildCreditCheckDto(processingContext.getBusinessKey(), processingContext.getFlowId(),
                        creditCheckRequest, responseJson, errorHandler, gson, checkDate)).orElseGet(GeneralProxyObsDto::new);

        creditCheckRepository.save(modelMapper.map(creditCheckDto, CreditCheckEntity.class));

        log.info("Saved CreditCheckEntity for businessKey {} and flowId {} into CREDIT_CHECK_ENTITY table",
                processingContext.getBusinessKey(), processingContext.getFlowId());
    }

    public GeneralProxyObsDto fetchCheckCredit(String businessKey, String flowId) {
        return creditCheckRepository.findTopByBusinessKeyAndFlowIdOrderByCreatedDateDesc(businessKey, flowId)
                .map(creditCheckEntity -> {
                    log.info("Fetching check credit entity with businessKey {} and flowId {}", businessKey, flowId);
                    return modelMapper.map(creditCheckEntity, GeneralProxyObsDto.class);
                }).orElseGet(() -> {
                    log.info("Credit check entity with businessKey {} and flowId {} not found", businessKey, flowId);
                    return new GeneralProxyObsDto();
                });
    }
}
