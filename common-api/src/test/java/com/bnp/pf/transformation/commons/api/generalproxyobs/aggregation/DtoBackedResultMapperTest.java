package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.message.input.Application;

class DtoBackedResultMapperTest {

    private final List<String> calls = new ArrayList<>();
    private final DtoBackedResultMapper<String> mapper = new DtoBackedResultMapper<>(ProxySource.CKI,
            result -> "dto:" + result.getResponseJson(), (dto, application) -> calls.add(String.valueOf(dto)));

    @Test
    void existingMapperIsCalledOnceForEachResult() {
        mapper.apply(Arrays.asList(result("a"), result("b")), new Application(), null);

        assertEquals(List.of("dto:a", "dto:b"), calls);
    }

    @Test
    void noResults_existingMapperIsCalledOnceWithNull() {
        mapper.apply(List.of(), new Application(), null);

        assertEquals(List.of("null"), calls);
    }

    @Test
    void sourceIsTheOneGiven() {
        assertEquals(ProxySource.CKI, mapper.source());
    }

    private static ProxyResultDto result(String responseJson) {
        ProxyResultDto result = new ProxyResultDto();
        result.setResponseJson(responseJson);
        return result;
    }
}
