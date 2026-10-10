package com.example.taskapi.task.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import com.example.taskapi.user.domain.Role;
import com.example.taskapi.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

/**
 * Runs against the real Flyway schema (Hibernate only validates it), so this also proves
 * the migration matches the entity mappings.
 * <p>
 * Uses the configured in-memory H2 ({@code Replace.NONE}): Boot's auto-replaced embedded
 * H2 broke the migration's CHECK constraints with H2 2.4.240.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TaskRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Autowired
    private TestEntityManager em;
    @Autowired
    private TaskRepository repository;

    private User alice;
    private User bob;
    private Task aliceTodo;
    private Task aliceDoneSoon;
    private Task aliceNoDue;
    private Task bobTask;

    @BeforeEach
    void setUp() {
        alice = em.persist(new User("alice", "hash", Role.USER));
        bob = em.persist(new User("bob", "hash", Role.USER));
        aliceTodo = em.persist(new Task(alice, "A todo", null, TaskStatus.TODO, TODAY.plusDays(10), NOW));
        aliceDoneSoon = em.persist(new Task(alice, "A done", null, TaskStatus.DONE, TODAY.plusDays(1), NOW));
        aliceNoDue = em.persist(new Task(alice, "A no due", null, TaskStatus.TODO, null, NOW));
        bobTask = em.persist(new Task(bob, "B todo", null, TaskStatus.TODO, TODAY.plusDays(1), NOW));
        em.flush();
        em.clear();
    }

    // ---- isolation ------------------------------------------------------------------

    @Test
    void ownerCanLoadOwnTask() {
        assertThat(repository.findByIdAndOwnerId(aliceTodo.getId(), alice.getId()))
                .get()
                .extracting(Task::getTitle)
                .isEqualTo("A todo");
    }

    @Test
    void usersCannotLoadEachOthersTasks() {
        assertThat(repository.findByIdAndOwnerId(bobTask.getId(), alice.getId())).isEmpty();
        assertThat(repository.findByIdAndOwnerId(aliceTodo.getId(), bob.getId())).isEmpty();
    }

    @Test
    void searchOnlyReturnsCallersTasks() {
        Page<Task> alicePage = repository.search(alice.getId(), null, null, PageRequest.of(0, 50));
        Page<Task> bobPage = repository.search(bob.getId(), null, null, PageRequest.of(0, 50));

        assertThat(alicePage.getContent()).extracting(Task::getId)
                .containsExactlyInAnyOrder(aliceTodo.getId(), aliceDoneSoon.getId(), aliceNoDue.getId());
        assertThat(bobPage.getContent()).extracting(Task::getId).containsExactly(bobTask.getId());
    }

    // ---- filters, paging, sorting -----------------------------------------------------

    @Test
    void filtersByStatus() {
        Page<Task> page = repository.search(alice.getId(), TaskStatus.TODO, null, PageRequest.of(0, 50));

        assertThat(page.getContent()).extracting(Task::getId)
                .containsExactlyInAnyOrder(aliceTodo.getId(), aliceNoDue.getId());
    }

    @Test
    void dueBeforeIsExclusiveAndExcludesTasksWithoutDueDate() {
        Page<Task> page = repository.search(alice.getId(), null, TODAY.plusDays(10), PageRequest.of(0, 50));

        assertThat(page.getContent()).extracting(Task::getId).containsExactly(aliceDoneSoon.getId());
    }

    @Test
    void combinesFilters() {
        Page<Task> page = repository.search(alice.getId(), TaskStatus.TODO, TODAY.plusDays(30),
                PageRequest.of(0, 50));

        assertThat(page.getContent()).extracting(Task::getId).containsExactly(aliceTodo.getId());
    }

    @Test
    void pagesAndSorts() {
        Page<Task> first = repository.search(alice.getId(), null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Task::getTitle).containsExactly("A done", "A no due");
    }

    // ---- optimistic locking -----------------------------------------------------------

    @Test
    void versionStartsAtZeroAndIncrementsOnUpdate() {
        Task loaded = repository.findByIdAndOwnerId(aliceTodo.getId(), alice.getId()).orElseThrow();
        assertThat(loaded.getVersion()).isZero();

        loaded.changeStatus(TaskStatus.IN_PROGRESS, NOW.plusSeconds(60));
        Task saved = repository.saveAndFlush(loaded);

        assertThat(saved.getVersion()).isEqualTo(1L);
    }

    @Test
    void staleCopyCannotOverwriteNewerVersion() {
        Task stale = repository.findByIdAndOwnerId(aliceTodo.getId(), alice.getId()).orElseThrow();
        em.detach(stale);

        // someone else updates the row in the meantime
        Task fresh = repository.findByIdAndOwnerId(aliceTodo.getId(), alice.getId()).orElseThrow();
        fresh.changeStatus(TaskStatus.DONE, NOW.plusSeconds(1));
        repository.saveAndFlush(fresh);
        em.clear();

        stale.changeStatus(TaskStatus.IN_PROGRESS, NOW.plusSeconds(2));
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
