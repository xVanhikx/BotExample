package org.example.dao;

import org.example.entity.AppUser;
import org.example.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDAO extends JpaRepository<Task, Long> {
    List<Task> findByUserAndCompleted(AppUser user, boolean completed);
    Task findByTitle(String title);
    Task findByTitleAndCompleted(String title, boolean completed);

    void removeByTitle(String title);
    List<Task> findByUser(AppUser user);
}
