package com.example.taskapi.common;

/** Mapped to 404. Also used when a resource exists but belongs to someone else. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
