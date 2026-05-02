package com.pranavgupta.todoapp.controller;

import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.dto.CreateTodoRequest;
import com.pranavgupta.todoapp.dto.TodoResponse;
import com.pranavgupta.todoapp.dto.TodoStatusFilter;
import com.pranavgupta.todoapp.dto.UpdateTodoRequest;
import com.pranavgupta.todoapp.service.TodoService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
 * <p>v1 hardcodes the owner of every todo to the seeded system user (id=1).
 * Phase 6 (auth) replaces {@link #currentUserId()} with a lookup from the
 * authenticated {@code Authentication} / {@code OidcUser} principal — that's
 * the entire diff for adding multi-user awareness here.</p>
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    /** Hardcoded for v1; Phase 6 replaces this with the authenticated user's id. */
    private static final long V1_SYSTEM_USER_ID = 1L;

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoResponse> list(
            @RequestParam(name = "status", defaultValue = "all") TodoStatusFilter status) {
        return todoService.findAllForUser(currentUserId(), status)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TodoResponse getOne(@PathVariable Long id) {
        return TodoResponse.from(todoService.findByIdForUser(currentUserId(), id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        Todo created = todoService.createForUser(currentUserId(), request.title());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(TodoResponse.from(created));
    }

    @PatchMapping("/{id}")
    public TodoResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTodoRequest request) {
        Todo updated = todoService.updateForUser(
                currentUserId(), id, request.title(), request.completed());
        return TodoResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.deleteForUser(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The single seam where authentication will plug in. Phase 6 replaces this
     * with a lookup from {@link org.springframework.security.core.Authentication}
     * (and an upsert of the {@code User} row keyed by OIDC {@code sub}).
     */
    private Long currentUserId() {
        return V1_SYSTEM_USER_ID;
    }
}
