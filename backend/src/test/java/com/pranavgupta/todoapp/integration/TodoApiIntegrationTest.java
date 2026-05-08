package com.pranavgupta.todoapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranavgupta.todoapp.TestcontainersConfiguration;
import com.pranavgupta.todoapp.domain.User;
import com.pranavgupta.todoapp.repository.TodoRepository;
import com.pranavgupta.todoapp.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.pranavgupta.todoapp.test.TestPrincipals.appOidcUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test: real Spring context, real Postgres
 * (Testcontainers), real Spring Security filter chain.
 *
 * <p>The marquee assertion — <strong>per-user data isolation</strong> — is
 * the headline of {@link #perUserIsolation_userBCannotSeeOrModifyUserAsTodos()}.
 * If this test passes, the contract that "user A's todos are never visible to
 * user B" is enforced through every layer of the stack.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TodoApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TodoRepository todoRepository;
    @Autowired private ObjectMapper objectMapper;

    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() {
        // Wipe + re-seed so each test starts clean. ON DELETE CASCADE on
        // todos.user_id means deleting users also clears their todos.
        userRepository.deleteAll();
        aliceId = userRepository.save(newUser("alice-sub", "alice@example.com", "Alice")).getId();
        bobId   = userRepository.save(newUser("bob-sub",   "bob@example.com",   "Bob")).getId();
    }

    @Test
    void crud_happyPath_endToEnd() throws Exception {
        // POST: Alice creates a todo
        String createdJson = mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Buy almond milk\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy almond milk"))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long todoId = objectMapper.readTree(createdJson).get("id").asLong();

        // GET list: Alice sees her one todo
        mockMvc.perform(get("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Buy almond milk"));

        // PATCH: toggle completed
        mockMvc.perform(patch("/api/todos/" + todoId)
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // GET active filter: empty (the only todo is now completed)
        mockMvc.perform(get("/api/todos?status=active")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // GET completed filter: contains the todo
        mockMvc.perform(get("/api/todos?status=completed")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // DELETE
        mockMvc.perform(delete("/api/todos/" + todoId)
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // GET list: empty again
        mockMvc.perform(get("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Sanity: row really gone
        assertThat(todoRepository.findById(todoId)).isEmpty();
    }

    @Test
    void perUserIsolation_userBCannotSeeOrModifyUserAsTodos() throws Exception {
        // Alice creates a todo
        String json = mockMvc.perform(post("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alice's secret todo\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long aliceTodoId = objectMapper.readTree(json).get("id").asLong();

        // Bob's GET list returns empty (he doesn't see Alice's todo)
        mockMvc.perform(get("/api/todos")
                        .with(oidcLogin().oidcUser(appOidcUser(bobId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Bob's GET by id: 404 (NOT 403 — by design, to avoid leaking
        // existence of resources that don't belong to him).
        mockMvc.perform(get("/api/todos/" + aliceTodoId)
                        .with(oidcLogin().oidcUser(appOidcUser(bobId))))
                .andExpect(status().isNotFound());

        // Bob's PATCH attempt: 404, no mutation
        mockMvc.perform(patch("/api/todos/" + aliceTodoId)
                        .with(oidcLogin().oidcUser(appOidcUser(bobId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"title\":\"hacked\"}"))
                .andExpect(status().isNotFound());

        // Bob's DELETE attempt: 404, no deletion
        mockMvc.perform(delete("/api/todos/" + aliceTodoId)
                        .with(oidcLogin().oidcUser(appOidcUser(bobId)))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        // Sanity: Alice's todo is still there, untouched
        var aliceTodoFromDb = todoRepository.findById(aliceTodoId);
        assertThat(aliceTodoFromDb).isPresent();
        assertThat(aliceTodoFromDb.get().getTitle()).isEqualTo("Alice's secret todo");
        assertThat(aliceTodoFromDb.get().isCompleted()).isFalse();
        assertThat(aliceTodoFromDb.get().getUserId()).isEqualTo(aliceId);
    }

    @Test
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void me_authenticated_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(oidcLogin().oidcUser(appOidcUser(aliceId, "alice@example.com", "Alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceId))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    private static User newUser(String externalId, String email, String displayName) {
        User u = new User();
        u.setExternalId(externalId);
        u.setEmail(email);
        u.setDisplayName(displayName);
        return u;
    }
}
