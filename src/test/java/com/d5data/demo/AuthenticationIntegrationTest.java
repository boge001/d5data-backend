package com.d5data.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserDetailsService users;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void unauthenticatedRequestReturns401InsteadOfRedirecting() throws Exception {
        mvc.perform(get("/api/hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void csrfEndpointIsPublicAndReturnsUsableMetadata() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void validLoginPersistsAuthenticationForTheNextRequest() throws Exception {
        MockHttpSession session = login();
        mvc.perform(get("/api/hello").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Hello World"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "test").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void missingCsrfIsRejectedEvenWithCorrectCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .param("username", "test").param("password", "123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesTheAuthenticatedSession() throws Exception {
        MockHttpSession session = login();
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/hello")).andExpect(status().isUnauthorized());
    }

    @Test
    void credentialsAloneDoNotAuthorizeAnUnrelatedRequest() throws Exception {
        login();
        mvc.perform(get("/api/hello")).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordsAreEncoded() {
        String stored = users.loadUserByUsername("test").getPassword();
        assertThat(stored).isNotEqualTo("123456");
        assertThat(encoder.matches("123456", stored)).isTrue();
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "test").param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn().getRequest().getSession(false);
    }
}
