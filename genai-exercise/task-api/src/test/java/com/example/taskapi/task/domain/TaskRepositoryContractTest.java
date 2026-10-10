package com.example.taskapi.task.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * Review finding #6: tenant isolation must not depend on callers remembering not to use
 * {@code findById}. The repository may only expose owner-scoped reads, so an unscoped lookup
 * does not compile.
 */
class TaskRepositoryContractTest {

    @Test
    void doesNotInheritUnscopedCrudOperations() {
        for (Class<?> unscoped : List.of(CrudRepository.class, ListCrudRepository.class,
                PagingAndSortingRepository.class, QueryByExampleExecutor.class)) {
            assertThat(unscoped.isAssignableFrom(TaskRepository.class))
                    .as("TaskRepository must not extend %s", unscoped.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void exposesOnlyOwnerScopedReadsAndTheWritesTheServiceNeeds() {
        assertThat(Arrays.stream(TaskRepository.class.getMethods()).map(Method::getName))
                .containsExactlyInAnyOrder("findByIdAndOwnerId", "search", "saveAndFlush", "delete");
    }

    @Test
    void everyReadTakesTheOwnerId() {
        Arrays.stream(TaskRepository.class.getMethods())
                .filter(m -> m.getName().startsWith("find") || m.getName().startsWith("search"))
                .forEach(m -> assertThat(Arrays.stream(m.getParameters()).map(Parameter::getName))
                        .as("parameters of %s", m.getName())
                        .contains("ownerId"));
    }
}
