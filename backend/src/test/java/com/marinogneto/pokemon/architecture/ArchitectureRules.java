package com.marinogneto.pokemon.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Clean Architecture rules (ADR-002), parameterised by root package so the same rules can be applied to the
 * production code ({@link ArchitectureTest}) and to the fixtures that prove they work ({@link ArchitectureRulesTest}).
 *
 * <p>Layers may legitimately be empty while the project grows (e.g. before the first adapter exists), so empty
 * layers are allowed; the self-test guarantees the rules are not vacuous.
 */
final class ArchitectureRules {

    /** Frameworks the inner layers (domain, application) must never depend on. */
    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "jakarta.persistence..",
        "jakarta.validation..",
        "org.hibernate..",
        "tools.jackson..",
        "com.fasterxml.jackson.."
    };

    private ArchitectureRules() {
    }

    /** adapters / infrastructure -> application -> domain; never the other way round. */
    static ArchRule layerDependencies(String root) {
        return layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain").definedBy(root + ".domain..")
                .layer("Application").definedBy(root + ".application..")
                .layer("Adapters").definedBy(root + ".adapters..")
                .layer("Infrastructure").definedBy(root + ".infrastructure..")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure");
    }

    /** The domain is plain Java: no Spring, JPA, Bean Validation, Hibernate or Jackson. */
    static ArchRule domainIsFrameworkFree(String root) {
        return frameworkFree(root + ".domain..", "the domain");
    }

    /** Use cases and ports are plain Java too; frameworks are wired in from infrastructure. */
    static ArchRule applicationIsFrameworkFree(String root) {
        return frameworkFree(root + ".application..", "the application layer");
    }

    /** Controllers reach data through use cases (ports), never by calling repositories or HTTP clients directly. */
    static ArchRule inboundAdaptersDoNotUseOutboundAdapters(String root) {
        return noClasses().that().resideInAPackage(root + ".adapters.in..")
                .should().dependOnClassesThat().resideInAPackage(root + ".adapters.out..")
                .because("inbound adapters must go through application use cases")
                .allowEmptyShould(true);
    }

    private static ArchRule frameworkFree(String packagePattern, String layerName) {
        return noClasses().that().resideInAPackage(packagePattern)
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
                .because(layerName + " must stay independent of frameworks (ADR-002)")
                .allowEmptyShould(true);
    }
}
