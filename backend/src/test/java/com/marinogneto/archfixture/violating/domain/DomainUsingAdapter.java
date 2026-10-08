package com.marinogneto.archfixture.violating.domain;

import com.marinogneto.archfixture.violating.adapters.in.web.ControllerUsingRepository;

/** Violation: the domain must not depend on an outer layer (adapters). */
public class DomainUsingAdapter {

    private ControllerUsingRepository controller;
}
