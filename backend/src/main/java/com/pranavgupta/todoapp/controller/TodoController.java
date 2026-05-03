package com.pranavgupta.todoapp.controller;

import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.dto.CreateTodoRequest;
import com.pranavgupta.todoapp.dto.TodoResponse;
import com.pranavgupta.todoapp.dto.TodoStatusFilter;
import com.pranavgupta.todoapp.dto.UpdateTodoRequest;
import com.pranavgupta.todoapp.security.AppOidcUser;
import com.pranavgupta.todoapp.service.TodoService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for the {@link Todo} resource.
 *
 * <p>Phase 6 made this class auth-aware: every method takes the authenticated
 * principal as {@code @AuthenticationPrincipal AppOidcUser}, and the user id
 * comes from {@code principal.getAppUserId()} (set during the login OAuth
 * callback by {@code CustomOidcUserService}). The service + repository layers
 * were already user-scoped, so this is the only file in the data path that
 * needed to change.</p>
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoResponse> list(
            @AuthenticationPrincipal AppOidcUser principal,
            @RequestParam(name = "status", defaultValue = "all") TodoStatusFilter status) {
        return todoService.findAllForUser(principal.getAppUserId(), status)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TodoResponse getOne(
            @AuthenticationPrincipal AppOidcUser principal,
            @PathVariable Long id) {
        return TodoResponse.from(todoService.findByIdForUser(principal.getAppUserId(), id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(
            @AuthenticationPrincipal AppOidcUser principal,
            @Valid @RequestBody CreateTodoRequest request) {
        Todo created = todoService.createForUser(principal.getAppUserId(), request.title());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(TodoResponse.from(created));
    }

    @PatchMapping("/{id}")
    public TodoResponse update(
            @AuthenticationPrincipal AppOidcUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request) {
        Todo updated = todoService.updateForUser(
                principal.getAppUserId(), id, request.title(), request.completed());
        return TodoResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppOidcUser principal,
            @PathVariable Long id) {
        todoService.deleteForUser(principal.getAppUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
