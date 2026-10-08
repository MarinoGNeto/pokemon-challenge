package com.marinogneto.archfixture.violating.domain;

import org.springframework.stereotype.Component;

/** Violation: the domain must not depend on Spring. */
@Component
public class DomainUsingSpring {
}
