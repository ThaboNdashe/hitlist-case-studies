package com.portfolio.hitlist.permutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.portfolio.hitlist.interfaces.AutomationStateRepository;
import com.portfolio.hitlist.quota.AutomationState;

/**
 * Production proof of ADR-002: Deep Pagination Exhaustion + Stateless Mathematical Permutation.
 *
 * Ensures that:
 * 1. Multi-campaign configurations are flattened deterministically into a Cartesian permutation matrix.
 * 2. The active target is resolved statelessly via (globalSearchIteration % totalPermutations).
 * 3. When an API response includes a nextPageToken, the cursor holds its current permutation index
 *    to exhaust Page 2..N on subsequent hourly ticks before incrementing globalSearchIteration.
 */
public class CampaignPermutationEngine {

    private final AutomationStateRepository stateRepository;

    public CampaignPermutationEngine(AutomationStateRepository stateRepository) {
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository must not be null");
    }

    /**
     * Flattens all active campaigns into an ordered Cartesian product of (Country x City x Category).
     */
    public List<SearchTarget> buildPermutations(List<SearchCampaignConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        List<SearchTarget> permutations = new ArrayList<>();
        for (SearchCampaignConfig config : configs) {
            if (!config.active()) {
                continue;
            }
            List<String> countries = config.countries().isEmpty() ? List.of("") : config.countries();
            for (String country : countries) {
                for (String city : config.cities()) {
                    for (String category : config.categories()) {
                        permutations.add(new SearchTarget(country.trim(), city.trim(), category.trim()));
                    }
                }
            }
        }
        return List.copyOf(permutations);
    }

    /**
     * Resolves the current search execution plan and updates the deep-exhaustion cursor state
     * based on whether the external API returned a follow-up nextPageToken.
     */
    public PermutationStepResult executeStep(List<SearchCampaignConfig> configs, String returnedNextPageToken) {
        List<SearchTarget> permutations = buildPermutations(configs);
        if (permutations.isEmpty()) {
            throw new IllegalStateException("No active search campaign permutations configured");
        }

        AutomationState state = stateRepository.findById(AutomationState.SINGLETON_ID)
            .orElseGet(() -> {
                AutomationState created = new AutomationState();
                created.setId(AutomationState.SINGLETON_ID);
                return stateRepository.save(created);
            });

        int currentIndex = Math.floorMod(state.getGlobalSearchIteration(), permutations.size());
        SearchTarget currentTarget = permutations.get(currentIndex);
        String usedPageToken = state.getNextPageToken();

        boolean hasMorePages = returnedNextPageToken != null && !returnedNextPageToken.isBlank();
        if (hasMorePages) {
            // Deep Exhaustion: retain same globalSearchIteration so the next hourly tick fetches the next page
            state.setNextPageToken(returnedNextPageToken.trim());
        } else {
            // Query exhausted: clear token and advance to the next permutation coordinate
            state.setNextPageToken(null);
            state.setGlobalSearchIteration(state.getGlobalSearchIteration() + 1);
        }
        stateRepository.save(state);

        return new PermutationStepResult(
            currentTarget,
            usedPageToken,
            state.getNextPageToken(),
            state.getGlobalSearchIteration(),
            permutations.size()
        );
    }

    public record PermutationStepResult(
            SearchTarget targetExecuted,
            String requestPageTokenUsed,
            String nextPageTokenStored,
            int nextGlobalSearchIteration,
            int totalPermutations
    ) {}
}
