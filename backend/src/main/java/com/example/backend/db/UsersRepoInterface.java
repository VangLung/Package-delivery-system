package com.example.backend.db;

import com.example.backend.models.User;

public interface UsersRepoInterface {
    public boolean createUser(User user);
    public User findByUsername(String username);
}
