package com.example.backend.db;

import java.util.Set;
import com.example.backend.models.User;

public interface UsersRepoInterface {
    public boolean createUser(User user);
    public User findByUsername(String username);
    public Set<String> findAllUsernames();
}
