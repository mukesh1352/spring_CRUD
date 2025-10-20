package com.example.demo.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.Model.TaskExecution;
import com.example.demo.Model.TaskModel;
import com.example.demo.Repository.TaskRepo;

@Service
public class TaskExecService {

    private final TaskRepo repo;

    public TaskExecService(TaskRepo repo){
        this.repo = repo;
    }

    // Get all tasks
    public List<TaskModel> getAllTasks(){
        return repo.findAll();
    }

    // Get task by ID
    public Optional<TaskModel> getTaskById(String id){
        return repo.findById(id);
    }

    // Find tasks by name (exact match)
    public List<TaskModel> findTaskByName(String name){
        return repo.findByName(name);
    }

    // Create or update a task
    public TaskModel createOrUpdateTask(TaskModel task) {
        if (task.getCommand() == null || !isSafeCommand(task.getCommand())) {
            throw new IllegalArgumentException("Unsafe or null command detected!");
        }
        return repo.save(task);
    }

    // Delete task
    public void deleteTask(String id){
        if(!repo.existsById(id)){
            throw new IllegalArgumentException("Task with id " + id + " not found");
        }
        repo.deleteById(id);
    }

    // Run a task command and save TaskExecution
    public TaskExecution runTask(String taskId){
        Optional<TaskModel> optionalTask = repo.findById(taskId);
        if(optionalTask.isEmpty()){
            throw new IllegalArgumentException("Task not found");
        }

        TaskModel task = optionalTask.get();
        TaskExecution execution = new TaskExecution();
        execution.setStartTime(new Date());

        StringBuilder output = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(task.getCommand());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch(Exception e){
            output.append("Error executing command: ").append(e.getMessage());
        } finally {
            execution.setEndTime(new Date());
            execution.setOutput(output.toString());
            task.getTaskExecutions().add(execution);
            repo.save(task);
        }

        return execution;
    }

    // Basic command safety check
    private boolean isSafeCommand(String command){
        String lower = command.toLowerCase();
        return !(lower.contains("rm") || lower.contains("shutdown") || lower.contains("reboot"));
    }
}
