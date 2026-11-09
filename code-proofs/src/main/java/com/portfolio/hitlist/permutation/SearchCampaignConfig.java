package com.portfolio.hitlist.permutation;

import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of an active multi-geographic search campaign.
 */
public record SearchCampaignConfig(
        String name,
        List<String> countries,
        List<String> cities,
        List<String> categories,
        boolean active
) {
    public SearchCampaignConfig {
        Objects.requireNonNull(name, "name must not be null");
        countries = countries == null ? List.of() : List.copyOf(countries);
        cities = cities == null ? List.of() : List.copyOf(cities);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
