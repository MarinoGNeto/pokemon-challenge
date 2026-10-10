package com.example.taskapi.task.service;

import java.time.LocalDate;

import com.example.taskapi.task.domain.TaskStatus;

/**
 * Service-layer inputs. Deliberately free of web/HTTP types and with no owner field:
 * the owner is always passed separately from the authenticated principal.
 */
public final class TaskCommands {

    private TaskCommands() {
    }

    /** @param status optional; defaults to {@link TaskStatus#TODO} */
    public record CreateTask(String title, String description, TaskStatus status, LocalDate dueDate) {
    }

    /** Full replacement. {@code description} and {@code dueDate} may be null to clear them. */
    public record UpdateTask(String title, String description, TaskStatus status, LocalDate dueDate,
                             long expectedVersion) {
    }

    public record ChangeStatus(TaskStatus status, long expectedVersion) {
    }

    /** Both fields optional. {@code dueBefore} is exclusive. */
    public record TaskFilter(TaskStatus status, LocalDate dueBefore) {

        public static TaskFilter none() {
            return new TaskFilter(null, null);
        }
    }
}
