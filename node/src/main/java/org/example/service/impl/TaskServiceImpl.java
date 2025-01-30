package org.example.service.impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.TaskDAO;
import org.example.entity.AppUser;
import org.example.entity.Task;
import org.example.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j
@Service
public class TaskServiceImpl implements TaskService {
    private final TaskDAO taskDAO;

    public TaskServiceImpl(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    @Override
    public Task createTask(AppUser user, String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setCompleted(false);
        task.setUser(user);
        return taskDAO.save(task);
    }

    @Override
    public List<Task> getTasksByUser(AppUser user) {
        return taskDAO.findByUserAndCompleted(user, false);
    }

    @Override
    @Transactional
    public Task markTaskAsCompletedWhenNotCompleted(AppUser user, String title) {
        Task task = taskDAO.findByTitleAndCompleted(title, false);
        if (task == null) {
            throw new RuntimeException("Задача отсутствует");
        }
        task.setCompleted(true);
        return taskDAO.save(task);
    }

    @Transactional
    public Task markTaskAsCompletedWhenNotCompletedById(AppUser user, Long id) {
        List<Task> tasks = getTasksByUser(user);
        Task task = null;
        for (Task task1 : tasks) {
            if (tasks.indexOf(task1) == id) {
                task = task1;
            }
        }
        if (task == null) {
            throw new RuntimeException("Задача отсутствует");
        }
        task.setCompleted(true);
        return taskDAO.save(task);
    }

    @Override
    @Transactional
    public void removeTask(String title) {
        taskDAO.removeByTitle(title);
    }
}
