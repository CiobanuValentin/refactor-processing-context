// Exemplu pentru un proxy sursa (bkrckiproxy): endpointul nou sta langa cel vechi pana migreaza rat si sed.

@GetMapping("/checkCki/results")
public ProxyResultsDto getCheckCkiResults(@RequestParam String businessKey, @RequestParam String flowId) {
    return mappingService.fetchCheckCkiResults(businessKey, flowId);
}

// In MappingService: toate randurile aplicatiei, nu doar primul.
public ProxyResultsDto fetchCheckCkiResults(String businessKey, String flowId) {
    ProxyResultsDto dto = new ProxyResultsDto();
    dto.setBusinessKey(businessKey);
    dto.setFlowId(flowId);
    // TODO: numele entitatii, al repository-ului si al getterelor sunt presupuse
    for (CheckCkiEntity entity : checkCkiRepository.findAllByBusinessKeyAndFlowId(businessKey, flowId)) {
        ProxyResultDto result = new ProxyResultDto();
        result.setRequestJson(entity.getSingleBkrRequestJson());
        result.setResponseJson(entity.getSingleBkrResponseJson());
        result.setErrorHandlerJson(entity.getErrorHandlerJson());
        dto.getResults().add(result);
    }
    return dto;
}
