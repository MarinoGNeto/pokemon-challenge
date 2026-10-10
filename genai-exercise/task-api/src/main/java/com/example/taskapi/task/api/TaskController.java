package com.example.taskapi.task.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import com.example.taskapi.security.CurrentUser;
import com.example.taskapi.task.api.TaskDtos.CreateTaskRequest;
import com.example.taskapi.task.api.TaskDtos.PageResponse;
import com.example.taskapi.task.api.TaskDtos.TaskResponse;
import com.example.taskapi.task.api.TaskDtos.UpdateTaskRequest;
import com.example.taskapi.task.api.TaskDtos.UpdateTaskStatusRequest;
import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskStatus;
import com.example.taskapi.task.service.TaskCommands.ChangeStatus;
import com.example.taskapi.task.service.TaskCommands.CreateTask;
import com.example.taskapi.task.service.TaskCommands.TaskFilter;
import com.example.taskapi.task.service.TaskCommands.UpdateTask;
import com.example.taskapi.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.create(CurrentUser.id(jwt), new CreateTask(
                request.title(), request.description(), request.status(), request.dueDate()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(task.getId())
                .toUri();
        return ResponseEntity.created(location).body(TaskResponse.from(task));
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(taskService
                .list(CurrentUser.id(jwt), new TaskFilter(status, dueBefore), pageable)
                .map(TaskResponse::from));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return TaskResponse.from(taskService.get(CurrentUser.id(jwt), id));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable UUID id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        Task task = taskService.update(CurrentUser.id(jwt), id, new UpdateTask(
                request.title(), request.description(), request.status(), request.dueDate(), request.version()));
        return TaskResponse.from(task);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        Task task = taskService.changeStatus(CurrentUser.id(jwt), id,
                new ChangeStatus(request.status(), request.version()));
        return TaskResponse.from(task);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        taskService.delete(CurrentUser.id(jwt), id);
    }
}
