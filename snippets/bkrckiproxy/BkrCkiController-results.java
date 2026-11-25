// Exemplu pentru un proxy sursa (bkrckiproxy): endpoint care intoarce toate randurile aplicatiei, in acelasi
// GeneralProxyObsDto ca endpointul actual. Endpointul vechi ramane pana migreaza rat si sed.
//
// DESCHIS: nu am vazut cum salveaza cki randurile (unul per persoana sau unul per aplicatie) si nici cum mapeaza
// entitatea in GeneralProxyObsDto. Corpul de mai jos presupune ca maparea existenta se poate aplica pe fiecare rand.

@GetMapping("/checkCki/results")
public List<GeneralProxyObsDto> getCheckCkiResults(@RequestParam String businessKey, @RequestParam String flowId) {
    return mappingService.fetchCheckCkiResults(businessKey, flowId);
}

// In MappingService:
public List<GeneralProxyObsDto> fetchCheckCkiResults(String businessKey, String flowId) {
    // TODO: numele repository-ului si al metodei sunt presupuse
    return checkCkiRepository.findAllByBusinessKeyAndFlowId(businessKey, flowId).stream()
            .map(entity -> modelMapper.map(entity, GeneralProxyObsDto.class))
            .toList();
}
