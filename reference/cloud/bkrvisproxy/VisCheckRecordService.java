// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisCheckRecordService {
    private final CheckVisEntityRepository checkVisEntityRepository;
    private final BuildDTOsUtility buildDTOsUtility;
    private final ModelMapper modelMapper;
    private final Gson gson;

    public void save(ProcessingContext processingContext, VisRequest visRequest, String responseJson,
                     List<Error> errors, LocalDate checkDate) {
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setErrors(new ArrayList<>(errors));

        GeneralProxyObsDto checkVisDto = Optional.ofNullable(buildDTOsUtility
                .createCheckVisDto(processingContext.getBusinessKey(), processingContext.getFlowId(),
                        visRequest, responseJson, errorHandler, checkDate, gson)).orElseGet(GeneralProxyObsDto::new);

        checkVisEntityRepository.save(modelMapper.map(checkVisDto, CheckVisEntity.class));
        log.info("Saved CheckVisEntity for businessKey {} and flowId{} into CHECK_VIS_ENTITY table",
                processingContext.getBusinessKey(), processingContext.getFlowId());
    }

    public List<GeneralProxyObsDto> fetchVisEntity(String businessKey, String flowId) {
        List<CheckVisEntity> checkVisEntities =
                checkVisEntityRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(businessKey, flowId);
        log.info("Found {} Check VIS entities for businessKey {} and flowId {}", checkVisEntities.size(), businessKey, flowId);

        // De observat: se mapeaza lista intreaga (checkVisEntities), nu elementul (checkVisEntity).
        return checkVisEntities
                .stream()
                .map(checkVisEntity -> modelMapper.map(checkVisEntities, GeneralProxyObsDto.class))
                .toList();
    }
}
