package com.pranavgupta.todoapp.controller;

import com.pranavgupta.todoapp.config.SecurityConfig;
import com.pranavgupta.todoapp.service.CustomOidcUserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.pranavgupta.todoapp.test.TestPrincipals.appOidcUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link MeController}. Smaller surface than
 * {@link TodoControllerTest} but exercises the same patterns.
 */
@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** SecurityConfig dependency; never invoked in this slice. */
    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @Test
    void me_authenticated_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(oidcLogin().oidcUser(appOidcUser(42L, "alice@example.com", "Alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void me_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().is3xxRedirection());
    }
}
