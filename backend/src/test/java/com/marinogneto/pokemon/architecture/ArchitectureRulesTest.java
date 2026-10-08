package com.marinogneto.pokemon.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the architecture rules themselves. An ArchUnit rule that silently matches nothing would pass forever,
 * so each rule is run against fixture code that deliberately breaks it (it must fail and name the offender)
 * and against a compliant fixture (it must pass). Fixtures live outside the application's base package so
 * Spring's component scan never sees them.
 */
class ArchitectureRulesTest {

    private static final String VIOLATING = "com.marinogneto.archfixture.violating";
    private static final String COMPLIANT = "com.marinogneto.archfixture.compliant";

    private static JavaClasses violatingClasses;
    private static JavaClasses compliantClasses;

    @BeforeAll
    static void importFixtures() {
        violatingClasses = new ClassFileImporter().importPackages(VIOLATING);
        compliantClasses = new ClassFileImporter().importPackages(COMPLIANT);
    }

    @Test
    void layerRuleRejectsDomainDependingOnAdapters() {
        assertViolation(ArchitectureRules.layerDependencies(VIOLATING), "DomainUsingAdapter");
    }

    @Test
    void layerRuleRejectsApplicationDependingOnInfrastructure() {
        assertViolation(ArchitectureRules.layerDependencies(VIOLATING), "UseCaseUsingInfrastructure");
    }

    @Test
    void domainMustNotUseFrameworks() {
        assertViolation(ArchitectureRules.domainIsFrameworkFree(VIOLATING), "DomainUsingSpring");
    }

    @Test
    void applicationMustNotUseFrameworks() {
        assertViolation(ArchitectureRules.applicationIsFrameworkFree(VIOLATING), "UseCaseUsingJpa");
    }

    @Test
    void inboundAdaptersMustNotCallOutboundAdaptersDirectly() {
        assertViolation(ArchitectureRules.inboundAdaptersDoNotUseOutboundAdapters(VIOLATING),
                "ControllerUsingRepository");
    }

    @Test
    void compliantCodePassesEveryRule() {
        for (ArchRule rule : allRules(COMPLIANT)) {
            assertThatCode(() -> rule.check(compliantClasses))
                    .as(rule.getDescription())
                    .doesNotThrowAnyException();
        }
    }

    private static void assertViolation(ArchRule rule, String offendingClass) {
        assertThatThrownBy(() -> rule.check(violatingClasses))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(offendingClass);
    }

    private static List<ArchRule> allRules(String root) {
        return List.of(
                ArchitectureRules.layerDependencies(root),
                ArchitectureRules.domainIsFrameworkFree(root),
                ArchitectureRules.applicationIsFrameworkFree(root),
                ArchitectureRules.inboundAdaptersDoNotUseOutboundAdapters(root));
    }
}
