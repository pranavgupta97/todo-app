package com.pranavgupta.todoapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranavgupta.todoapp.config.SecurityConfig;
import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.dto.CreateTodoRequest;
import com.pranavgupta.todoapp.dto.TodoStatusFilter;
import com.pranavgupta.todoapp.exception.TodoNotFoundException;
import com.pranavgupta.todoapp.service.CustomOidcUserService;
import com.pranavgupta.todoapp.service.TodoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.pranavgupta.todoapp.test.TestPrincipals.appOidcUser;
import static com.pranavgupta.todoapp.test.TestTodos.todo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

/**
 * Web-slice tests for {@link TodoController}.
 *
 * <p>{@code @WebMvcTest} loads only the web layer; we {@code @Import}
 * {@link SecurityConfig} so the real authorization rules + CSRF wiring apply.
 * {@link TodoService} is mocked, so we test only the controller's HTTP
 * behaviour: routing, validation, status codes, JSON shape, and the security
 * boundary.</p>
 *
 * <p>The {@code test} profile keeps CSRF on (matching the prod posture); the
 * dev profile's CSRF-off override does not apply here.</p>
 */
@WebMvcTest(TodoController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TodoControllerTest {

    private static final Long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TodoService todoService;

    /** Required because SecurityConfig depends on it; never actually invoked in slice tests. */
    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    // ==================================================================== //
    //  Authorization boundary                                              //
    // ==================================================================== //

    @Test
    void anyEndpoint_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().is3xxRedirection());
    }

    // ==================================================================== //
    //  GET /api/todos                                                      //
    // ==================================================================== //

    @Test
    void list_default_returnsAllTodos() throws Exception {
        when(todoService.findAllForUser(USER_ID, TodoStatusFilter.ALL))
                .thenReturn(List.of(todo(1L, USER_ID, "first"), todo(2L, USER_ID, "second")));

        mockMvc.perform(get("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("first"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void list_withStatusActive_routesActiveFilter() throws Exception {
        when(todoService.findAllForUser(USER_ID, TodoStatusFilter.ACTIVE))
                .thenReturn(List.of(todo(1L, USER_ID, "active task")));

        mockMvc.perform(get("/api/todos?status=active")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("active task"));

        verify(todoService).findAllForUser(USER_ID, TodoStatusFilter.ACTIVE);
    }

    @Test
    void list_withInvalidStatus_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/todos?status=banana")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ==================================================================== //
    //  GET /api/todos/{id}                                                 //
    // ==================================================================== //

    @Test
    void getOne_existing_returns200() throws Exception {
        when(todoService.findByIdForUser(USER_ID, 1L)).thenReturn(todo(1L, USER_ID, "found"));

        mockMvc.perform(get("/api/todos/1")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("found"));
    }

    @Test
    void getOne_missing_returns404ProblemDetail() throws Exception {
        when(todoService.findByIdForUser(USER_ID, 999L))
                .thenThrow(new TodoNotFoundException(999L));

        mockMvc.perform(get("/api/todos/999")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Todo not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.todoId").value(999));
    }

    // ==================================================================== //
    //  POST /api/todos                                                     //
    // ==================================================================== //

    @Test
    void create_validRequest_returns201WithLocation() throws Exception {
        when(todoService.createForUser(USER_ID, "new task"))
                .thenReturn(todo(7L, USER_ID, "new task"));

        mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTodoRequest("new task"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/todos/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("new task"));
    }

    @Test
    void create_withoutCsrfToken_returns403() throws Exception {
        // Demonstrates that CSRF really is enforced under the test profile —
        // the dev profile's CSRF-off override doesn't apply here. The Phase 11
        // frontend will handle the X-XSRF-TOKEN dance on real users' behalf.
        mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        // intentionally no .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_emptyTitle_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void create_titleOver255Chars_returns400() throws Exception {
        String longTitle = "a".repeat(300);
        mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unauthenticated_returns3xx() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().is3xxRedirection());
    }

    // ==================================================================== //
    //  PATCH /api/todos/{id}                                               //
    // ==================================================================== //

    @Test
    void update_validRequest_returns200() throws Exception {
        Todo updated = todo(1L, USER_ID, "updated title", true);
        when(todoService.updateForUser(USER_ID, 1L, "updated title", true)).thenReturn(updated);

        mockMvc.perform(patch("/api/todos/1")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated title\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("updated title"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void update_missingTodo_returns404() throws Exception {
        when(todoService.updateForUser(eq(USER_ID), eq(999L), any(), any()))
                .thenThrow(new TodoNotFoundException(999L));

        mockMvc.perform(patch("/api/todos/999")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(patch("/api/todos/1")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isForbidden());
    }

    // ==================================================================== //
    //  DELETE /api/todos/{id}                                              //
    // ==================================================================== //

    @Test
    void delete_existingTodo_returns204() throws Exception {
        mockMvc.perform(delete("/api/todos/1")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(todoService).deleteForUser(USER_ID, 1L);
    }

    @Test
    void delete_missingTodo_returns404() throws Exception {
        doThrow(new TodoNotFoundException(999L))
                .when(todoService).deleteForUser(USER_ID, 999L);

        mockMvc.perform(delete("/api/todos/999")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(delete("/api/todos/1")
                        .with(oidcLogin().oidcUser(appOidcUser(USER_ID))))
                .andExpect(status().isForbidden());
    }
}
