package com.example.taskapi.task.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;

/**
 * Request/response records for the task endpoints. None of them carries an owner:
 * ownership comes exclusively from the authenticated principal.
 */
public final class TaskDtos {

    private TaskDtos() {
    }

    /**
     * The "due date not in the past" rule is enforced in the service (against the
     * application Clock) rather than with {@code @FutureOrPresent}, so there is a
     * single source of truth for "today".
     */
    public record CreateTaskRequest(
            @NotBlank @Size(max = Task.TITLE_MAX) String title,
            @Size(max = Task.DESCRIPTION_MAX) String description,
            TaskStatus status,
            LocalDate dueDate) {
    }

    public record UpdateTaskRequest(
            @NotBlank @Size(max = Task.TITLE_MAX) String title,
            @Size(max = Task.DESCRIPTION_MAX) String description,
            @NotNull TaskStatus status,
            LocalDate dueDate,
            @NotNull @PositiveOrZero Long version) {
    }

    public record UpdateTaskStatusRequest(
            @NotNull TaskStatus status,
            @NotNull @PositiveOrZero Long version) {
    }

    public record TaskResponse(
            UUID id,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            Instant createdAt,
            Instant updatedAt,
            long version) {

        public static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getDueDate(),
                    task.getCreatedAt(),
                    task.getUpdatedAt(),
                    task.getVersion());
        }
    }

    /** Stable pagination envelope (instead of serializing Spring's PageImpl). */
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<String> sort) {

        public static <T> PageResponse<T> from(Page<T> page) {
            List<String> sort = page.getSort().stream()
                    .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                    .toList();
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), sort);
        }
    }
}
