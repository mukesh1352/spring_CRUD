package com.example.demo.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.Model.TaskExecution;
import com.example.demo.Model.TaskModel;
import com.example.demo.Repository.TaskRepo;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;

@Service
public class TaskExecService {

    private final TaskRepo repo;

    public TaskExecService(TaskRepo repo){
        this.repo = repo;
    }

    // Get all tasks
    public List<TaskModel> getAllTasks() {
        return repo.findAll();
    }

    // Get task by ID
    public TaskModel getTaskById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    // Find tasks by name
    public List<TaskModel> findTaskByName(String name) {
        return repo.findByName(name);
    }

    // Create or update task
    public TaskModel createOrUpdateTask(TaskModel task) {
        validateCommand(task.getCommand());
        return repo.save(task);
    }

    // Delete task
    public void deleteTask(String id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Task with id " + id + " not found");
        }
        repo.deleteById(id);
    }

    // Run task inside Kubernetes pod
    public TaskExecution runTaskInPod(String taskId, String namespace, String podName, String containerName) {
        TaskModel task = repo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        validateCommand(task.getCommand());

        TaskExecution execution = new TaskExecution();
        execution.setStartTime(new Date());
        StringBuilder output = new StringBuilder();

        try {
            ApiClient client = ClientBuilder.defaultClient();
            Configuration.setDefaultApiClient(client);
            Exec exec = new Exec();

            // This opens the exec channel in the pod
            Process proc = exec.exec(
                    namespace,
                    podName,
                    new String[]{"/bin/sh", "-c", task.getCommand()},
                    containerName,
                    true,  // stdin
                    true   // tty
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            boolean finished = proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                output.append("\n[Process timed out after 30s]");
            }

            if (output.length() > 5000) {
                output = new StringBuilder(output.substring(0, 5000) + "\n...[output truncated]");
            }

        } catch (Exception e) {
            output.append("Error executing command in pod: ").append(e.getMessage());
        } finally {
            execution.setEndTime(new Date());
            execution.setOutput(output.toString());
            task.getTaskExecutions().add(execution);
            repo.save(task);
        }

        return execution;
    }

    // Validate safe commands
    private void validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        String[] blacklist = {"rm", "reboot", "shutdown", "kill", "dd", "mkfs"};
        for (String bad : blacklist) {
            if (command.toLowerCase().contains(bad)) {
                throw new IllegalArgumentException("Unsafe command detected: " + bad);
            }
        }

        String[] whitelist = {"echo", "ls", "cat", "date", "pwd"};
        boolean allowed = false;
        for (String safe : whitelist) {
            if (command.equals(safe) || command.startsWith(safe + " ")) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalArgumentException("Command not allowed. Allowed: echo, ls, cat, date, pwd");
        }
    }
}
