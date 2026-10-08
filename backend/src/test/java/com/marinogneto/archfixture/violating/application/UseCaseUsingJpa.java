package com.marinogneto.archfixture.violating.application;

import jakarta.persistence.EntityManager;

/** Violation: the application layer must not depend on JPA. */
public class UseCaseUsingJpa {

    private EntityManager entityManager;
}
