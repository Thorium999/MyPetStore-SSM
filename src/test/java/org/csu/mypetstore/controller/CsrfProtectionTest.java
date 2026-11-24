package org.csu.mypetstore.controller;

import org.csu.mypetstore.security.CsrfToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CSRF 防护：页面必须带令牌，写操作缺令牌 / 令牌不符 / 跨会话令牌都要被 403 挡住。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pagesCarryHiddenTokenField() throws Exception {
        mockMvc.perform(get("/signon"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void tokenIsStableWithinSession() throws Exception {
        MvcResult first = mockMvc.perform(get("/signon")).andReturn();
        MockHttpSession session = (MockHttpSession) first.getRequest().getSession();
        String token = (String) session.getAttribute(CsrfToken.SESSION_KEY);

        mockMvc.perform(get("/signon").session(session));

        assertThat(token).isNotBlank();
        assertThat(session.getAttribute(CsrfToken.SESSION_KEY)).isEqualTo(token);
    }

    @Test
    void rejectsPostWithoutToken() throws Exception {
        mockMvc.perform(post("/signon")
                                .session(new MockHttpSession())
                                .param("username", "alice")
                                .param("password", "PetStore123!"))
               .andExpect(status().isForbidden());
    }

    @Test
    void rejectsTokenFromAnotherSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, "token-of-this-session");

        mockMvc.perform(post("/signon")
                                .session(session)
                                .param("_csrf", "token-from-elsewhere")
                                .param("username", "alice")
                                .param("password", "PetStore123!"))
               .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWhenSessionHasNoToken() throws Exception {
        mockMvc.perform(post("/signon")
                                .session(new MockHttpSession())
                                .param("_csrf", "anything")
                                .param("username", "alice")
                                .param("password", "PetStore123!"))
               .andExpect(status().isForbidden());
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, "matching-token");

        mockMvc.perform(post("/signon")
                                .session(session)
                                .param("_csrf", "matching-token")
                                .param("username", "alice")
                                .param("password", "PetStore123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/main"));
    }

    @Test
    void readOnlyRequestsNeedNoToken() throws Exception {
        mockMvc.perform(get("/main")).andExpect(status().isOk());
        mockMvc.perform(get("/category")).andExpect(status().isOk());
    }
}
