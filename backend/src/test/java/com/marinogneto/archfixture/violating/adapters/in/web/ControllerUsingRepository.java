package com.marinogneto.archfixture.violating.adapters.in.web;

import com.marinogneto.archfixture.violating.adapters.out.persistence.SomeRepository;

/** Violation: an inbound adapter must go through a use case, not call an outbound adapter directly. */
public class ControllerUsingRepository {

    private SomeRepository repository;
}
