package com.example.demo.Repository;

import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.Model.TaskModel;

public interface TaskRepo extends MongoRepository<TaskModel,String>{
    List<TaskModel> findByName(String name);
}

