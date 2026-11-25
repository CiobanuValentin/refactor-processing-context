package com.bnp.pf.transformation.visproxy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Error, ErrorHandler, CheckVisEntity, CheckVisEntityRepository, BuildDTOsUtility si
// VisRequest sunt presupuse - pastreaza-le pe cele din clasa actuala.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.visproxy.entity.CheckVisEntity;
import com.bnp.pf.transformation.visproxy.repository.CheckVisEntityRepository;
import com.bnp.pf.transformation.visproxy.utils.BuildDTOsUtility;

import urn.bkr.wsdl.vis.VisRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisCheckRecordService {
    private final CheckVisEntityRepository checkVisEntityRepository;
    private final BuildDTOsUtility buildDTOsUtility;
    private final ModelMapper modelMapper;
    private final Gson gson;

    public void save(ProxyContext ctx, VisRequest visRequest, String responseJson,
                     List<Error> errors, LocalDate checkDate) {
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setErrors(new ArrayList<>(errors));

        GeneralProxyObsDto checkVisDto = Optional.ofNullable(buildDTOsUtility
                .createCheckVisDto(ctx.getBusinessKey(), ctx.getFlowId(),
                        visRequest, responseJson, errorHandler, checkDate, gson)).orElseGet(GeneralProxyObsDto::new);

        checkVisEntityRepository.save(modelMapper.map(checkVisDto, CheckVisEntity.class));
        log.info("Saved CheckVisEntity for businessKey {} and flowId{} into CHECK_VIS_ENTITY table",
                ctx.getBusinessKey(), ctx.getFlowId());
    }

    public List<GeneralProxyObsDto> fetchVisEntity(String businessKey, String flowId) {
        List<CheckVisEntity> checkVisEntities =
                checkVisEntityRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(businessKey, flowId);
        log.info("Found {} Check VIS entities for businessKey {} and flowId {}", checkVisEntities.size(), businessKey, flowId);

        // FIX fata de codul actual: se mapeaza fiecare entitate; inainte se mapa lista intreaga (checkVisEntities)
        // pentru fiecare element, deci dto-urile nu primeau campurile randului.
        return checkVisEntities
                .stream()
                .map(checkVisEntity -> modelMapper.map(checkVisEntity, GeneralProxyObsDto.class))
                .toList();
    }
}
