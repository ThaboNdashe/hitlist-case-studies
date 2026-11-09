package com.portfolio.hitlist.permutation;

/**
 * Represents a single resolved (Country, City, Category) permutation coordinate.
 */
public record SearchTarget(String country, String city, String category) {

    public String toSearchQuery() {
        String base = category + " in " + city;
        if (country != null && !country.isBlank()) {
            return base + ", " + country;
        }
        return base;
    }
}
