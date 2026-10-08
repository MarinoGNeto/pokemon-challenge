package com.marinogneto.archfixture.violating.application;

import com.marinogneto.archfixture.violating.infrastructure.SomeConfiguration;

/** Violation: the application layer must not depend on infrastructure. */
public class UseCaseUsingInfrastructure {

    private SomeConfiguration configuration;
}
