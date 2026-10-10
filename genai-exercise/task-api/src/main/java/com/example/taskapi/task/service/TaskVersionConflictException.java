package com.example.taskapi.task.service;

import java.util.UUID;

import com.example.taskapi.common.ConflictException;

public class TaskVersionConflictException extends ConflictException {

    public TaskVersionConflictException(UUID taskId, long expected, long actual) {
        super("Task " + taskId + " was modified concurrently (expected version " + expected
                + ", current version " + actual + ")");
    }
}
