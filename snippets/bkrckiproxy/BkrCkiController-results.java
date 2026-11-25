// bkrckiproxy: endpoint care intoarce toate randurile aplicatiei, in acelasi GeneralProxyObsDto ca endpointul actual.
// Confirmat din MappingService.checkCki (cloud): se salveaza cate un rand pentru fiecare client eligibil,
// iar fetchCheckCkiRequestAndResponse intoarce doar ultimul (findTop...OrderByCreationDesc).
// Endpointul vechi ramane pana migreaza rat si sed. Cand un agregator trece pe liste, in application.yml-ul lui
// se schimba doar valoarea: bkrcki.endpoint: "/checkCki/results".

@GetMapping("/checkCki/results")
public List<GeneralProxyObsDto> getCheckCkiResults(@RequestParam String businessKey, @RequestParam String flowId) {
    return mappingService.fetchCheckCkiResults(businessKey, flowId);
}

// In MappingService, langa fetchCheckCkiRequestAndResponse:
public List<GeneralProxyObsDto> fetchCheckCkiResults(String businessKey, String flowId) {
    // TODO: metoda de repository trebuie adaugata in CheckCkiRequestAndResponseRepository.
    // DESCHIS: la o a doua procesare a aceleiasi aplicatii raman si randurile vechi, deci acelasi client apare
    // de doua ori. De vazut cum citea ApplicationBuilder-ul din legacy (toate randurile sau ultimul per client).
    return ckiRequestRepo.findAllByBusinessKeyAndFlowIdOrderByCreationAsc(businessKey, flowId).stream()
            .map(entity -> modelMapper.map(entity, GeneralProxyObsDto.class))
            .toList();
}
