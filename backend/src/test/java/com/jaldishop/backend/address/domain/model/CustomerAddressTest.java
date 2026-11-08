package com.jaldishop.backend.address.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerAddressTest {
    @ParameterizedTest
    @CsvSource({
            "' AV.  JOSÉ PARDO 100 DPTO. 201 ',150122,true",
            "Av. José Pardo 100 Dpto. 202,150122,false",
            "Av. Jose Pardo 100 Dpto. 201,150122,false",
            "Av José Pardo 100 Dpto. 201,150122,false",
            "Avenida José Pardo 100 Dpto. 201,150122,false",
            "Av. José Pardo 10 Dpto. 0201,150122,false",
            "Av. José Pardo 100 Dpto. 201,150101,false"
    })
    void shouldCompareOnlyConservativelyNormalizedStreetAndUbigeo(String street, String ubigeo, boolean same) {
        var address = CustomerAddress.create(UUID.randomUUID(), "Casa", "Juan", "987654321",
                "Av. José Pardo 100 Dpto. 201", "Lima", "Lima", "Miraflores", "150122",
                "Portón verde", null, null, true);
        assertThat(address.hasSameLocation(ubigeo, street)).isEqualTo(same);
    }
}
