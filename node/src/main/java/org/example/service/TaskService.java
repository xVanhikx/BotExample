package org.example.service;

import org.example.entity.AppUser;
import org.example.entity.Task;

import java.util.List;

public interface TaskService {
    Task createTask(AppUser user, String title);
    List<Task> getTasksByUser(AppUser user);
    Task markTaskAsCompletedWhenNotCompleted(AppUser user, String title);
    Task markTaskAsCompletedWhenNotCompletedById(AppUser user, Long id);
    void removeTask(String title);
}
