package com.example.demo.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.Model.TaskModel;
import com.example.demo.Model.TaskExecution;
import com.example.demo.Service.TaskExecService;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskExecService taskService;

    public TaskController(TaskExecService taskService) {
        this.taskService = taskService;
    }

    // Create a new task
    @PostMapping
    public ResponseEntity<TaskModel> createTask(@RequestBody TaskModel task) {
        try {
            TaskModel created = taskService.createOrUpdateTask(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Update a task
    @PutMapping
    public ResponseEntity<TaskModel> putTask(@RequestBody TaskModel task) {
        try {
            TaskModel saved = taskService.createOrUpdateTask(task);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get all tasks
    @GetMapping
    public List<TaskModel> getAllTasks() {
        return taskService.getAllTasks();
    }

    // Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskModel> getTaskById(@PathVariable String id) {
        try {
            TaskModel task = taskService.getTaskById(id);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Search tasks by name
    @GetMapping("/search")
    public ResponseEntity<List<TaskModel>> findTasksByName(@RequestParam String name) {
        List<TaskModel> tasks = taskService.findTaskByName(name);
        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(tasks);
    }

    // Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Run task in Kubernetes pod
    @PutMapping("/{id}/run")
    public ResponseEntity<TaskExecution> runTask(
            @PathVariable String id,
            @RequestParam String namespace,
            @RequestParam String podName,
            @RequestParam String containerName) {
        try {
            TaskExecution execution = taskService.runTaskInPod(id, namespace, podName, containerName);
            return ResponseEntity.ok(execution);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
