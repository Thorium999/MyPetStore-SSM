package org.csu.mypetstore.controller;

import org.csu.mypetstore.security.CsrfToken;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录流程的接口级测试：会话写入、跳转目标、失败文案、站外跳转防护。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    private static final String CSRF = "test-token";


    private static final String PASSWORD = "PetStore123!";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageRenders() throws Exception {
        mockMvc.perform(get("/signon"))
               .andExpect(status().isOk())
               .andExpect(content().string(org.hamcrest.Matchers.containsString("登录")));
    }

    @Test
    void loginPageKeepsReturnUrlForRoundTrip() throws Exception {
        mockMvc.perform(get("/signon").param("returnUrl", "/category?categoryId=1"))
               .andExpect(status().isOk())
               .andExpect(model().attribute("returnUrl", "/category?categoryId=1"))
               .andExpect(content().string(containsString("name=\"returnUrl\"")))
               .andExpect(content().string(containsString("/category?categoryId=1")));
    }

    @Test
    void loginPageDropsUnsafeReturnUrl() throws Exception {
        mockMvc.perform(get("/signon").param("returnUrl", "//evil.example.com"))
               .andExpect(status().isOk())
               .andExpect(model().attributeDoesNotExist("returnUrl"))
               .andExpect(content().string(not(containsString("name=\"returnUrl\""))));
    }

    @Test
    void headerCarriesCurrentPageAsReturnUrl() throws Exception {
        // Thymeleaf 会把 = 和 & 编码，/ 与 ? 保持原样
        mockMvc.perform(get("/category"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("returnUrl=/category")));

        // 注意用带查询串的 URL：.param() 不会写进 getQueryString()
        mockMvc.perform(get("/category?categoryId=1"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("returnUrl=/category?categoryId%3D1")));
    }

    @Test
    void refusesReturnUrlPointingAtAnAction() throws Exception {
        // /signout 是动作不是页面：不能当回跳目标，否则登录完立刻被登出
        mockMvc.perform(get("/signon").param("returnUrl", "/signout"))
               .andExpect(status().isOk())
               .andExpect(model().attributeDoesNotExist("returnUrl"));

        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", PASSWORD)
                                .param("returnUrl", "/signout"))
               .andExpect(redirectedUrl("/main"));
    }

    @Test
    void signoutRejectsGet() throws Exception {
        mockMvc.perform(get("/signout"))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void signoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, CSRF);
        session.setAttribute(LoginSession.KEY,
                new LoginUser(1L, "alice", "林同学", "CUSTOMER"));

        mockMvc.perform(post("/signout").param("_csrf", CSRF).session(session))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/main"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void successfulLoginWritesSessionAndRedirectsHome() throws Exception {
        MvcResult result = mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                                   .param("username", "alice")
                                                   .param("password", PASSWORD))
                                  .andExpect(status().is3xxRedirection())
                                  .andExpect(redirectedUrl("/main"))
                                  .andReturn();

        LoginUser loggedIn = (LoginUser) result.getRequest().getSession().getAttribute(LoginSession.KEY);
        assertThat(loggedIn).isNotNull();
        assertThat(loggedIn.username()).isEqualTo("alice");
        assertThat(loggedIn.isAdmin()).isFalse();
    }

    @Test
    void successfulLoginRespectsInternalReturnUrl() throws Exception {
        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", PASSWORD)
                                .param("returnUrl", "/category?categoryId=1"))
               .andExpect(redirectedUrl("/category?categoryId=1"));
    }

    @Test
    void refusesOffsiteReturnUrl() throws Exception {
        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", PASSWORD)
                                .param("returnUrl", "//evil.example.com"))
               .andExpect(redirectedUrl("/main"));

        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", PASSWORD)
                                .param("returnUrl", "https://evil.example.com"))
               .andExpect(redirectedUrl("/main"));

        // 浏览器会把 /\host 当成 //host，必须一并挡掉
        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", PASSWORD)
                                .param("returnUrl", "/\\evil.example.com"))
               .andExpect(redirectedUrl("/main"));
    }

    @Test
    void wrongPasswordShowsGenericError() throws Exception {
        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "alice")
                                .param("password", "wrong"))
               .andExpect(status().isOk())
               .andExpect(model().attribute("error", "用户名或密码错误"))
               .andExpect(content().string(org.hamcrest.Matchers.containsString("用户名或密码错误")));
    }

    @Test
    void disabledAccountShowsDisabledMessage() throws Exception {
        mockMvc.perform(post("/signon").param("_csrf", CSRF).session(tokenSession())
                                .param("username", "disabled")
                                .param("password", PASSWORD))
               .andExpect(status().isOk())
               .andExpect(model().attribute("error", "该账号已被禁用，请联系管理员"));
    }

    /** 只带 CSRF 令牌、未登录的会话，用来验证"未登录"这条分支。 */
    private MockHttpSession tokenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, CSRF);
        return session;
    }
}
