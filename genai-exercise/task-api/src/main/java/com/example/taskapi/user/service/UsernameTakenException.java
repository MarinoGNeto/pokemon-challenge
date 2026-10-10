package com.example.taskapi.user.service;

import com.example.taskapi.common.ConflictException;

public class UsernameTakenException extends ConflictException {

    public UsernameTakenException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
