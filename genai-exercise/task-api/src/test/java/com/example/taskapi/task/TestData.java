package com.example.taskapi.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskStatus;
import com.example.taskapi.user.domain.Role;
import com.example.taskapi.user.domain.User;

import org.springframework.test.util.ReflectionTestUtils;

/** Builders for entities in unit/slice tests where JPA does not assign ids/versions. */
public final class TestData {

    private TestData() {
    }

    public static User user(UUID id, String username) {
        User user = new User(username, "{noop}hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Task task(User owner, UUID id, long version, String title, TaskStatus status,
                            LocalDate dueDate, Instant now) {
        Task task = new Task(owner, title, null, status, dueDate, now);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "version", version);
        return task;
    }
}
