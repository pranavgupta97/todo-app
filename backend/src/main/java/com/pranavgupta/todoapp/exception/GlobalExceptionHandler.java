package com.pranavgupta.todoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Maps application-specific exceptions to RFC 7807 {@link ProblemDetail}
 * responses.
 *
 * <p>Spring Boot 3's default exception handling already covers the common
 * cases — bean-validation errors ({@code MethodArgumentNotValidException}),
 * malformed JSON, type mismatches in path/query binding, etc. — and emits
 * {@code application/problem+json} responses out of the box. We only need to
 * map exceptions that are unique to this app.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TodoNotFoundException.class)
    public ProblemDetail handleTodoNotFound(TodoNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Todo not found");
        problem.setType(URI.create("https://todo-app.local/problems/todo-not-found"));
        problem.setProperty("todoId", ex.getTodoId());
        return problem;
    }
}
