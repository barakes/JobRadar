package com.jobradar.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryResolverTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Israel                 | Israel",
            "Tel Aviv               | Israel",
            "Tel-Aviv, Israel       | Israel",
            "TEL AVIV               | Israel",
            "Petah Tikva            | Israel",
            "Ra'anana               | Israel",
            "Remote - Israel        | Israel",
            "New York, NY           | United States",
            "Remote, US             | United States",
            "London, UK             | United Kingdom",
            "Sofia                  | Bulgaria",
            "Singapore, Singapore   | Singapore"
    })
    void resolvesCountryFromLocation(String location, String expectedCountry) {
        assertThat(CountryResolver.resolve(location)).isEqualTo(expectedCountry);
    }

    @Test
    void prefersIsraelWhenSeveralCountriesAreListed() {
        assertThat(CountryResolver.resolve("London or Tel Aviv")).isEqualTo("Israel");
    }

    @Test
    void doesNotMatchShortCodesInsideOtherWords() {
        // "us" is inside "Austin", but Austin should still resolve through its own keyword.
        assertThat(CountryResolver.resolve("Austin, TX")).isEqualTo("United States");
        // "uk" is inside "Ukraine": must resolve to Ukraine, not the United Kingdom.
        assertThat(CountryResolver.resolve("Kyiv, Ukraine")).isEqualTo("Ukraine");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Remote", "Anywhere"})
    void returnsNullWhenCountryIsUnknown(String location) {
        assertThat(CountryResolver.resolve(location)).isNull();
    }
}