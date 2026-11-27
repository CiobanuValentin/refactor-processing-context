package com.bnp.pf.transformation.rvdrproxy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Error, ErrorHandler, CreditCheckEntity, CreditCheckRepository, BuildDTOsUtility si
// CreditCheckRequest sunt presupuse - pastreaza-le pe cele din clasa actuala.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.rvdrproxy.entity.CreditCheckEntity;
import com.bnp.pf.transformation.rvdrproxy.repository.CreditCheckRepository;
import com.bnp.pf.transformation.rvdrproxy.utils.BuildDTOsUtility;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCheckRecordService {
    private final CreditCheckRepository creditCheckRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;

    /** Neschimbat, cu ProxyContext in loc de ProcessingContext. */
    public void save(ProxyContext ctx, CreditCheckRequest creditCheckRequest,
                     String responseJson, List<Error> errors, LocalDate checkDate) {
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setErrors(new ArrayList<>(errors));

        GeneralProxyObsDto creditCheckDto = Optional.ofNullable(
                BuildDTOsUtility.buildCreditCheckDto(ctx.getBusinessKey(), ctx.getFlowId(),
                        creditCheckRequest, responseJson, errorHandler, gson, checkDate)).orElseGet(GeneralProxyObsDto::new);

        creditCheckRepository.save(modelMapper.map(creditCheckDto, CreditCheckEntity.class));

        log.info("Saved CreditCheckEntity for businessKey {} and flowId {} into CREDIT_CHECK_ENTITY table",
                ctx.getBusinessKey(), ctx.getFlowId());
    }

    /**
     * Toate randurile pentru businessKey si flowId, cel mai nou primul. Fara randuri intoarce lista goala
     * (inainte: un singur dto, iar fara randuri un dto gol).
     */
    public List<GeneralProxyObsDto> fetchCheckCredits(String businessKey, String flowId) {
        List<CreditCheckEntity> creditCheckEntities =
                creditCheckRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(businessKey, flowId);
        log.info("Found {} credit check entities for businessKey {} and flowId {}",
                creditCheckEntities.size(), businessKey, flowId);

        return creditCheckEntities
                .stream()
                .map(creditCheckEntity -> modelMapper.map(creditCheckEntity, GeneralProxyObsDto.class))
                .toList();
    }
}
