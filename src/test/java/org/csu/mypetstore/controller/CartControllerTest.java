package org.csu.mypetstore.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.mapper.CartItemMapper;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 购物车的 HTTP 层：未登录拦截、加购意图暂存、登录后正常渲染。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartControllerTest {

    private static final String CSRF = "test-token";


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CartItemMapper cartItemMapper;

    @Test
    void cartPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/cart"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/signon?returnUrl=%2Fcart"));
    }

    @Test
    void addToCartWhileLoggedOutRemembersIntent() throws Exception {
        MvcResult result = mockMvc.perform(post("/cart").param("_csrf", CSRF).session(tokenSession())
                                                   .param("productId", "22")
                                                   .param("quantity", "2"))
                                  .andExpect(status().is3xxRedirection())
                                  .andExpect(redirectedUrl("/signon?returnUrl=%2Fproduct%3Fid%3D22"))
                                  .andReturn();

        Object pending = result.getRequest().getSession().getAttribute("pendingCartAdd");
        assertThat(pending).isNotNull();
    }

    @Test
    void loggedInUserSeesCartWithIssues() throws Exception {
        clearCart(1L);
        addDirectly(1L, 21L, 1);
        addDirectly(1L, 30L, 1);   // DRAFT：页面应标出"已下架"
        addDirectly(1L, 34L, 1);   // 库存 0：页面应标出"已售罄"

        mockMvc.perform(get("/cart").session(aliceSession()))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("购物车")))
               .andExpect(content().string(containsString("已下架")))
               .andExpect(content().string(containsString("已售罄")));
    }

    @Test
    void loggedInAddRedirectsToCartWithNotice() throws Exception {
        mockMvc.perform(post("/cart").param("_csrf", CSRF)
                                .session(aliceSession())
                                .param("productId", "22")
                                .param("quantity", "1"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/cart"))
               .andExpect(flash().attributeExists("notice"));
    }

    @Test
    void updateAndDeleteRequireLogin() throws Exception {
        mockMvc.perform(post("/cart/1/quantity").param("_csrf", CSRF).session(tokenSession()).param("quantity", "2"))
               .andExpect(redirectedUrl("/signon?returnUrl=%2Fcart%2F1%2Fquantity"));

        mockMvc.perform(post("/cart/1/delete").param("_csrf", CSRF).session(tokenSession()))
               .andExpect(redirectedUrl("/signon?returnUrl=%2Fcart%2F1%2Fdelete"));
    }

    private void clearCart(Long userId) {
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    private void addDirectly(Long userId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setVersion(0L);
        cartItemMapper.insert(item);
    }

    private MockHttpSession aliceSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, CSRF);
        session.setAttribute(LoginSession.KEY, new LoginUser(1L, "alice", "林同学", "CUSTOMER"));
        return session;
    }

    /** 只带 CSRF 令牌、未登录的会话，用来验证"未登录"这条分支。 */
    private MockHttpSession tokenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfToken.SESSION_KEY, CSRF);
        return session;
    }
}
