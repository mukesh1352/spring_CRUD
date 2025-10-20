package com.example.demo.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.Model.TaskModel;
import com.example.demo.Model.TaskExecution; // Import from the same package
import com.example.demo.Service.TaskExecService;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskExecService taskService;

    public TaskController(TaskExecService taskService) {
        this.taskService = taskService;
    }

    // ✅ POST /tasks - create a new task
    @PostMapping
    public ResponseEntity<TaskModel> createTask(@RequestBody TaskModel task) {
        try {
            TaskModel created = taskService.createOrUpdateTask(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /tasks - get all tasks
    @GetMapping
    public List<TaskModel> getAllTasks() {
        return taskService.getAllTasks();
    }

    // GET /tasks/{id} - get a task by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskModel> getTaskById(@PathVariable String id) {
        Optional<TaskModel> task = taskService.getTaskById(id);
        return task.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // GET /tasks/search?name=xyz - find tasks by name
    @GetMapping("/search")
    public ResponseEntity<List<TaskModel>> findTasksByName(@RequestParam String name) {
        List<TaskModel> tasks = taskService.findTaskByName(name);
        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(tasks);
    }

    // DELETE /tasks/{id} - delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // PUT /tasks/{id}/run - run a task command
    @PutMapping("/{id}/run")
    public ResponseEntity<TaskExecution> runTask(@PathVariable String id) {
        try {
            TaskExecution execution = taskService.runTask(id);
            return ResponseEntity.ok(execution);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
