package com.example.taskapi.task.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.example.taskapi.common.BusinessRuleViolationException;
import com.example.taskapi.common.BusinessRuleViolationException.FieldViolation;
import com.example.taskapi.common.UnknownUserException;
import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskRepository;
import com.example.taskapi.task.domain.TaskStatus;
import com.example.taskapi.task.service.TaskCommands.ChangeStatus;
import com.example.taskapi.task.service.TaskCommands.CreateTask;
import com.example.taskapi.task.service.TaskCommands.TaskFilter;
import com.example.taskapi.task.service.TaskCommands.UpdateTask;
import com.example.taskapi.user.domain.User;
import com.example.taskapi.user.domain.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for tasks. Every operation takes the caller's id as {@code ownerId};
 * tasks owned by anyone else are reported as not found.
 */
@Service
@Transactional
public class TaskService {

    /** Entity properties a client may sort by. Anything else is rejected with 400. */
    public static final Set<String> SORTABLE_PROPERTIES =
            Set.of("createdAt", "updatedAt", "dueDate", "title", "status");

    private final TaskRepository tasks;
    private final UserRepository users;
    private final Clock clock;

    public TaskService(TaskRepository tasks, UserRepository users, Clock clock) {
        this.tasks = tasks;
        this.users = users;
        this.clock = clock;
    }

    public Task create(UUID ownerId, CreateTask command) {
        Objects.requireNonNull(ownerId, "ownerId");
        String title = normalizeTitle(command.title());
        List<FieldViolation> violations = new ArrayList<>();
        validateTitle(title, violations);
        validateDescription(command.description(), violations);
        if (command.dueDate() != null && command.dueDate().isBefore(today())) {
            violations.add(new FieldViolation("dueDate", "must not be in the past"));
        }
        throwIfAny(violations);

        User owner = users.findById(ownerId).orElseThrow(UnknownUserException::new);
        TaskStatus status = command.status() != null ? command.status() : TaskStatus.TODO;
        Task task = new Task(owner, title, command.description(), status, command.dueDate(), now());
        return tasks.saveAndFlush(task);
    }

    @Transactional(readOnly = true)
    public Task get(UUID ownerId, UUID taskId) {
        return loadOwned(ownerId, taskId);
    }

    @Transactional(readOnly = true)
    public Page<Task> list(UUID ownerId, TaskFilter filter, Pageable pageable) {
        Objects.requireNonNull(ownerId, "ownerId");
        validateSort(pageable.getSort());
        TaskFilter f = filter != null ? filter : TaskFilter.none();
        return tasks.search(ownerId, f.status(), f.dueBefore(), pageable);
    }

    public Task update(UUID ownerId, UUID taskId, UpdateTask command) {
        Task task = loadOwned(ownerId, taskId);
        checkVersion(task, command.expectedVersion());

        String title = normalizeTitle(command.title());
        List<FieldViolation> violations = new ArrayList<>();
        validateTitle(title, violations);
        validateDescription(command.description(), violations);
        if (command.status() == null) {
            violations.add(new FieldViolation("status", "must not be null"));
        }
        // A past due date is only rejected when it is being changed; an already-overdue
        // task must stay editable without forcing the client to move its date.
        LocalDate newDue = command.dueDate();
        if (newDue != null && !newDue.equals(task.getDueDate()) && newDue.isBefore(today())) {
            violations.add(new FieldViolation("dueDate", "must not be in the past"));
        }
        throwIfAny(violations);

        task.replace(title, command.description(), command.status(), newDue, now());
        return tasks.saveAndFlush(task);
    }

    public Task changeStatus(UUID ownerId, UUID taskId, ChangeStatus command) {
        if (command.status() == null) {
            throw new BusinessRuleViolationException("status", "must not be null");
        }
        Task task = loadOwned(ownerId, taskId);
        checkVersion(task, command.expectedVersion());
        task.changeStatus(command.status(), now());
        return tasks.saveAndFlush(task);
    }

    public void delete(UUID ownerId, UUID taskId) {
        Task task = loadOwned(ownerId, taskId);
        tasks.delete(task);
    }

    // ---------------------------------------------------------------------------------

    private Task loadOwned(UUID ownerId, UUID taskId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(taskId, "taskId");
        return tasks.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    /**
     * Rejects stale writes up-front. Hibernate's own @Version check (on flush) still
     * covers the race between this check and commit; that surfaces as
     * ObjectOptimisticLockingFailureException, also mapped to 409.
     */
    private static void checkVersion(Task task, long expectedVersion) {
        long actual = task.getVersion();
        if (actual != expectedVersion) {
            throw new TaskVersionConflictException(task.getId(), expectedVersion, actual);
        }
    }

    private static String normalizeTitle(String title) {
        return title == null ? null : title.strip();
    }

    private static void validateTitle(String title, List<FieldViolation> violations) {
        if (title == null || title.isEmpty()) {
            violations.add(new FieldViolation("title", "must not be blank"));
        } else if (title.length() > Task.TITLE_MAX) {
            violations.add(new FieldViolation("title", "size must be between 1 and " + Task.TITLE_MAX));
        }
    }

    private static void validateDescription(String description, List<FieldViolation> violations) {
        if (description != null && description.length() > Task.DESCRIPTION_MAX) {
            violations.add(new FieldViolation("description",
                    "size must be between 0 and " + Task.DESCRIPTION_MAX));
        }
    }

    private static void validateSort(Sort sort) {
        List<FieldViolation> violations = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                violations.add(new FieldViolation("sort",
                        "unsupported sort property '" + order.getProperty() + "'; allowed: "
                                + String.join(", ", SORTABLE_PROPERTIES.stream().sorted().toList())));
            }
        }
        throwIfAny(violations);
    }

    private static void throwIfAny(List<FieldViolation> violations) {
        if (!violations.isEmpty()) {
            throw new BusinessRuleViolationException(violations);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Truncated to the column precision so responses match what is persisted. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
