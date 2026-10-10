package com.example.taskapi.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.taskapi.common.BusinessRuleViolationException;
import com.example.taskapi.common.BusinessRuleViolationException.FieldViolation;
import com.example.taskapi.common.UnknownUserException;
import com.example.taskapi.task.TestData;
import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskRepository;
import com.example.taskapi.task.domain.TaskStatus;
import com.example.taskapi.task.service.TaskCommands.ChangeStatus;
import com.example.taskapi.task.service.TaskCommands.CreateTask;
import com.example.taskapi.task.service.TaskCommands.TaskFilter;
import com.example.taskapi.task.service.TaskCommands.UpdateTask;
import com.example.taskapi.user.domain.User;
import com.example.taskapi.user.domain.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    /** "Now" is 2026-06-15T10:00Z, so "today" is 2026-06-15. */
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-06-01T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    @Mock
    private TaskRepository tasks;
    @Mock
    private UserRepository users;

    private TaskService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, users, Clock.fixed(NOW, ZoneOffset.UTC));
        owner = TestData.user(OWNER_ID, "alice");
    }

    private void saveReturnsArgument() {
        when(tasks.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Task existingTask(long version, LocalDate dueDate) {
        return TestData.task(owner, TASK_ID, version, "Original", TaskStatus.TODO, dueDate, CREATED);
    }

    private static List<FieldViolation> violationsOf(Throwable t) {
        return ((BusinessRuleViolationException) t).getViolations();
    }

    // =================================================================================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void ownerIsTheAuthenticatedCaller() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("Write tests", "desc", TaskStatus.IN_PROGRESS,
                    TODAY.plusDays(3)));

            ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
            verify(tasks).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getOwner()).isSameAs(owner);
            assertThat(created.getTitle()).isEqualTo("Write tests");
            assertThat(created.getDescription()).isEqualTo("desc");
            assertThat(created.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(created.getDueDate()).isEqualTo(TODAY.plusDays(3));
        }

        @Test
        void timestampsComeFromTheClock() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("t", null, null, null));

            assertThat(created.getCreatedAt()).isEqualTo(NOW);
            assertThat(created.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        void statusDefaultsToTodo() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("t", null, null, null));

            assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void titleIsTrimmed() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("  padded  ", null, null, null));

            assertThat(created.getTitle()).isEqualTo("padded");
        }

        @Test
        void dueDateTodayIsAllowed() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("t", null, null, TODAY));

            assertThat(created.getDueDate()).isEqualTo(TODAY);
        }

        @Test
        void dueDateIsOptional() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("t", null, null, null));

            assertThat(created.getDueDate()).isNull();
        }

        @Test
        void dueDateInThePastIsRejected() {
            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask("t", null, null, TODAY.minusDays(1))))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e))
                            .containsExactly(new FieldViolation("dueDate", "must not be in the past")));
            verify(tasks, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t\n"})
        void blankTitleIsRejected(String title) {
            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask(title, null, null, null)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("title"));
            verify(tasks, never()).saveAndFlush(any());
        }

        @Test
        void nullTitleIsRejected() {
            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask(null, null, null, null)))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        void titleOf120CharsIsAllowed() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("x".repeat(120), null, null, null));

            assertThat(created.getTitle()).hasSize(120);
        }

        @Test
        void titleOf121CharsIsRejected() {
            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask("x".repeat(121), null, null, null)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("title"));
        }

        @Test
        void descriptionOf2000CharsIsAllowed() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            saveReturnsArgument();

            Task created = service.create(OWNER_ID, new CreateTask("t", "d".repeat(2000), null, null));

            assertThat(created.getDescription()).hasSize(2000);
        }

        @Test
        void descriptionOf2001CharsIsRejected() {
            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask("t", "d".repeat(2001), null, null)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("description"));
        }

        @Test
        void allViolationsAreReportedTogether() {
            assertThatThrownBy(() -> service.create(OWNER_ID,
                    new CreateTask(" ", "d".repeat(2001), null, TODAY.minusDays(1))))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactlyInAnyOrder("title", "description", "dueDate"));
        }

        @Test
        void callerThatNoLongerExistsIsRejected() {
            when(users.findById(OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(OWNER_ID, new CreateTask("t", null, null, null)))
                    .isInstanceOf(UnknownUserException.class);
            verify(tasks, never()).saveAndFlush(any());
        }
    }

    // =================================================================================

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        void returnsOwnTask() {
            Task task = existingTask(0, null);
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));

            assertThat(service.get(OWNER_ID, TASK_ID)).isSameAs(task);
        }

        @Test
        void anotherUsersTaskIsNotFound() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OTHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(OTHER_ID, TASK_ID))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(TASK_ID.toString());
            // never falls back to an unscoped lookup
            verify(tasks, never()).findById(any());
        }
    }

    // =================================================================================

    @Nested
    @DisplayName("list")
    class ListTasks {

        @Test
        void isScopedToCallerAndPassesFilters() {
            Pageable pageable = PageRequest.of(1, 5, Sort.by("dueDate"));
            Page<Task> page = new PageImpl<>(List.of(existingTask(0, null)), pageable, 6);
            when(tasks.search(OWNER_ID, TaskStatus.DONE, TODAY, pageable)).thenReturn(page);

            Page<Task> result = service.list(OWNER_ID, new TaskFilter(TaskStatus.DONE, TODAY), pageable);

            assertThat(result).isSameAs(page);
        }

        @Test
        void nullFilterMeansNoFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            when(tasks.search(OWNER_ID, null, null, pageable)).thenReturn(Page.empty(pageable));

            assertThat(service.list(OWNER_ID, null, pageable)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"createdAt", "updatedAt", "dueDate", "title", "status"})
        void whitelistedSortPropertiesAreAccepted(String property) {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(property));
            when(tasks.search(eq(OWNER_ID), any(), any(), eq(pageable))).thenReturn(Page.empty(pageable));

            assertThat(service.list(OWNER_ID, TaskFilter.none(), pageable)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"owner", "owner.username", "version", "nope"})
        void otherSortPropertiesAreRejected(String property) {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(property));

            assertThatThrownBy(() -> service.list(OWNER_ID, TaskFilter.none(), pageable))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("sort"));
            verify(tasks, never()).search(any(), any(), any(), any());
        }
    }

    // =================================================================================

    @Nested
    @DisplayName("update (PUT)")
    class Update {

        @Test
        void replacesAllFieldsAndBumpsUpdatedAt() {
            Task task = existingTask(3, TODAY.plusDays(1));
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));
            saveReturnsArgument();

            Task updated = service.update(OWNER_ID, TASK_ID,
                    new UpdateTask(" New ", "new desc", TaskStatus.DONE, TODAY.plusDays(10), 3));

            assertThat(updated)
                    .extracting(Task::getTitle, Task::getDescription, Task::getStatus, Task::getDueDate,
                            Task::getCreatedAt, Task::getUpdatedAt)
                    .containsExactly("New", "new desc", TaskStatus.DONE, TODAY.plusDays(10), CREATED, NOW);
            assertThat(updated.getOwner()).isSameAs(owner);
        }

        @Test
        void omittedOptionalFieldsAreCleared() {
            Task task = existingTask(0, TODAY.plusDays(1));
            task.replace("Original", "old desc", TaskStatus.TODO, TODAY.plusDays(1), CREATED);
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));
            saveReturnsArgument();

            Task updated = service.update(OWNER_ID, TASK_ID, new UpdateTask("t", null, TaskStatus.TODO, null, 0));

            assertThat(updated.getDescription()).isNull();
            assertThat(updated.getDueDate()).isNull();
        }

        @Test
        void anotherUsersTaskIsNotFound() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OTHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(OTHER_ID, TASK_ID,
                    new UpdateTask("t", null, TaskStatus.TODO, null, 0)))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(tasks, never()).saveAndFlush(any());
        }

        @Test
        void staleVersionIsAConflict() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(4, null)));

            assertThatThrownBy(() -> service.update(OWNER_ID, TASK_ID,
                    new UpdateTask("t", null, TaskStatus.TODO, null, 3)))
                    .isInstanceOf(TaskVersionConflictException.class)
                    .hasMessageContaining("expected version 3")
                    .hasMessageContaining("current version 4");
            verify(tasks, never()).saveAndFlush(any());
        }

        @Test
        void futureVersionIsAlsoAConflict() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(1, null)));

            assertThatThrownBy(() -> service.update(OWNER_ID, TASK_ID,
                    new UpdateTask("t", null, TaskStatus.TODO, null, 2)))
                    .isInstanceOf(TaskVersionConflictException.class);
        }

        @Test
        void changingDueDateToThePastIsRejected() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingTask(0, TODAY.plusDays(5))));

            assertThatThrownBy(() -> service.update(OWNER_ID, TASK_ID,
                    new UpdateTask("t", null, TaskStatus.TODO, TODAY.minusDays(1), 0)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("dueDate"));
            verify(tasks, never()).saveAndFlush(any());
        }

        @Test
        void keepingAnAlreadyPastDueDateIsAllowed() {
            LocalDate overdue = TODAY.minusDays(7);
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(0, overdue)));
            saveReturnsArgument();

            Task updated = service.update(OWNER_ID, TASK_ID,
                    new UpdateTask("Renamed", null, TaskStatus.IN_PROGRESS, overdue, 0));

            assertThat(updated.getDueDate()).isEqualTo(overdue);
            assertThat(updated.getTitle()).isEqualTo("Renamed");
        }

        @Test
        void nullStatusIsRejected() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(0, null)));

            assertThatThrownBy(() -> service.update(OWNER_ID, TASK_ID, new UpdateTask("t", null, null, null, 0)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactly("status"));
        }

        @Test
        void invalidTitleAndDescriptionAreRejected() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(0, null)));

            assertThatThrownBy(() -> service.update(OWNER_ID, TASK_ID,
                    new UpdateTask("x".repeat(121), "d".repeat(2001), TaskStatus.TODO, null, 0)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e)).extracting(FieldViolation::field)
                            .containsExactlyInAnyOrder("title", "description"));
        }
    }

    // =================================================================================

    @Nested
    @DisplayName("changeStatus (PATCH)")
    class ChangeStatusTests {

        @Test
        void changesOnlyTheStatus() {
            Task task = existingTask(2, TODAY.plusDays(1));
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));
            saveReturnsArgument();

            Task updated = service.changeStatus(OWNER_ID, TASK_ID, new ChangeStatus(TaskStatus.DONE, 2));

            assertThat(updated)
                    .extracting(Task::getStatus, Task::getTitle, Task::getDueDate, Task::getUpdatedAt)
                    .containsExactly(TaskStatus.DONE, "Original", TODAY.plusDays(1), NOW);
        }

        @Test
        void staleVersionIsAConflict() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(existingTask(2, null)));

            assertThatThrownBy(() -> service.changeStatus(OWNER_ID, TASK_ID, new ChangeStatus(TaskStatus.DONE, 1)))
                    .isInstanceOf(TaskVersionConflictException.class);
            verify(tasks, never()).saveAndFlush(any());
        }

        @Test
        void anotherUsersTaskIsNotFound() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OTHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(OTHER_ID, TASK_ID, new ChangeStatus(TaskStatus.DONE, 0)))
                    .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        void nullStatusIsRejected() {
            assertThatThrownBy(() -> service.changeStatus(OWNER_ID, TASK_ID, new ChangeStatus(null, 0)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(e -> assertThat(violationsOf(e))
                            .extracting(FieldViolation::field, FieldViolation::message)
                            .containsExactly(tuple("status", "must not be null")));
        }
    }

    // =================================================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        void deletesOwnTask() {
            Task task = existingTask(0, null);
            when(tasks.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));

            service.delete(OWNER_ID, TASK_ID);

            verify(tasks).delete(task);
        }

        @Test
        void anotherUsersTaskIsNotFoundAndNotDeleted() {
            when(tasks.findByIdAndOwnerId(TASK_ID, OTHER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(OTHER_ID, TASK_ID))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(tasks, never()).delete(any());
            verify(tasks, never()).deleteById(any());
        }
    }
}
