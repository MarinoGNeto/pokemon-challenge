package com.example.taskapi.task.service;

import java.util.UUID;

import com.example.taskapi.common.ResourceNotFoundException;

/**
 * Thrown both when the task does not exist and when it belongs to another user, so the
 * API never reveals whether someone else's task id is valid.
 */
public class TaskNotFoundException extends ResourceNotFoundException {

    public TaskNotFoundException(UUID taskId) {
        super("Task " + taskId + " not found");
    }
}
