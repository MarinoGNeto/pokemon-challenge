package com.marinogneto.pokemon.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the Clean Architecture dependency rule on the production code (ADR-002):
 * {@code adapters / infrastructure -> application -> domain}, never the other way round.
 * Breaking it fails {@code ./mvnw test}.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT = "com.marinogneto.pokemon";

    @ArchTest
    static final ArchRule layerDependencies = ArchitectureRules.layerDependencies(ROOT);

    @ArchTest
    static final ArchRule domainIsFrameworkFree = ArchitectureRules.domainIsFrameworkFree(ROOT);

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = ArchitectureRules.applicationIsFrameworkFree(ROOT);

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters =
            ArchitectureRules.inboundAdaptersDoNotUseOutboundAdapters(ROOT);
}
