package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

class DtoBackedResultMapperTest {

    private final List<List<String>> calls = new ArrayList<>();
    private final DtoBackedResultMapper<String> mapper = new DtoBackedResultMapper<>(ProxySource.CKI,
            result -> "dto:" + result.getFlowId(), (dtos, application) -> calls.add(dtos));

    @Test
    void mapperIsCalledOnceWithAllResults() {
        mapper.apply(Arrays.asList(result("a"), result("b")), new Application(), null);

        assertEquals(List.of(List.of("dto:a", "dto:b")), calls);
    }

    @Test
    void noResults_mapperIsCalledOnceWithEmptyList() {
        mapper.apply(List.of(), new Application(), null);

        assertEquals(List.of(List.of()), calls);
    }

    @Test
    void sourceIsTheOneGiven() {
        assertEquals(ProxySource.CKI, mapper.source());
    }

    private static GeneralProxyObsDto result(String flowId) {
        GeneralProxyObsDto result = new GeneralProxyObsDto();
        result.setFlowId(flowId);
        return result;
    }
}
