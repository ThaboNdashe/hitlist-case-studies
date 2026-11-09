package com.portfolio.hitlist.permutation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.portfolio.hitlist.fakes.FakeAutomationStateRepository;
import com.portfolio.hitlist.permutation.CampaignPermutationEngine.PermutationStepResult;

class CampaignPermutationEngineTest {

    private FakeAutomationStateRepository stateRepository;
    private CampaignPermutationEngine engine;

    @BeforeEach
    void setUp() {
        stateRepository = new FakeAutomationStateRepository();
        engine = new CampaignPermutationEngine(stateRepository);
    }

    @Test
    void holdsCurrentPermutationIndexDuringDeepPaginationBeforeAdvancingModuloCursor() {
        List<SearchCampaignConfig> campaigns = List.of(
            new SearchCampaignConfig(
                "Turkey Home Services",
                List.of("Turkey"),
                List.of("Istanbul", "Ankara"),
                List.of("Plumber", "Electrician"),
                true
            )
        );

        // Hour 1: Searches index 0 (Istanbul - Plumber), Google returns page token "token-page-2"
        PermutationStepResult step1 = engine.executeStep(campaigns, "token-page-2");
        assertThat(step1.targetExecuted().toSearchQuery()).isEqualTo("Plumber in Istanbul, Turkey");
        assertThat(step1.requestPageTokenUsed()).isNull();
        assertThat(step1.nextPageTokenStored()).isEqualTo("token-page-2");
        assertThat(step1.nextGlobalSearchIteration()).isEqualTo(0); // Held for deep exhaustion!

        // Hour 2: Still on index 0 (Istanbul - Plumber) using "token-page-2", Google returns null token (exhausted)
        PermutationStepResult step2 = engine.executeStep(campaigns, null);
        assertThat(step2.targetExecuted().toSearchQuery()).isEqualTo("Plumber in Istanbul, Turkey");
        assertThat(step2.requestPageTokenUsed()).isEqualTo("token-page-2");
        assertThat(step2.nextPageTokenStored()).isNull();
        assertThat(step2.nextGlobalSearchIteration()).isEqualTo(1); // Advanced to next permutation!

        // Hour 3: Moves to index 1 (Istanbul - Electrician)
        PermutationStepResult step3 = engine.executeStep(campaigns, null);
        assertThat(step3.targetExecuted().toSearchQuery()).isEqualTo("Electrician in Istanbul, Turkey");
        assertThat(step3.nextGlobalSearchIteration()).isEqualTo(2);
    }

    @Test
    void wrapsAroundSeamlesslyUsingStatelessModuloArithmeticAcrossMultipleActiveCampaigns() {
        List<SearchCampaignConfig> campaigns = List.of(
            new SearchCampaignConfig("Campaign A", List.of("Turkey"), List.of("Istanbul"), List.of("Plumber"), true),
            new SearchCampaignConfig("Inactive Campaign", List.of("Germany"), List.of("Berlin"), List.of("Roofing"), false),
            new SearchCampaignConfig("Campaign B", List.of("UK"), List.of("London"), List.of("Dentist"), true)
        );

        PermutationStepResult tick1 = engine.executeStep(campaigns, null);
        PermutationStepResult tick2 = engine.executeStep(campaigns, null);
        PermutationStepResult tick3 = engine.executeStep(campaigns, null); // Modulo wrap-around (2 % 2 == 0)

        assertThat(tick1.targetExecuted().toSearchQuery()).isEqualTo("Plumber in Istanbul, Turkey");
        assertThat(tick2.targetExecuted().toSearchQuery()).isEqualTo("Dentist in London, UK");
        assertThat(tick3.targetExecuted().toSearchQuery()).isEqualTo("Plumber in Istanbul, Turkey");
        assertThat(tick3.totalPermutations()).isEqualTo(2);
    }
}
