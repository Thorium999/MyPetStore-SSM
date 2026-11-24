package org.csu.mypetstore.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.mapper.CartItemMapper;
import org.csu.mypetstore.service.CartService;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 结算与订单的 HTTP 层：登录拦截、空车/不可结算的提示、下单成功跳详情、越权访问 404。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CartItemMapper cartItemMapper;
    @Autowired
    private CartService cartService;

    @Test
    void checkoutRequiresLogin() throws Exception {
        mockMvc.perform(get("/checkout"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/signon?returnUrl=%2Fcheckout"));
    }

    @Test
    void ordersRequireLogin() throws Exception {
        mockMvc.perform(get("/orders"))
               .andExpect(redirectedUrl("/signon?returnUrl=%2Forders"));
    }

    @Test
    void emptyCartCannotCheckout() throws Exception {
        clearCart(ALICE);

        mockMvc.perform(get("/checkout").session(sessionFor(ALICE)))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/cart"));
    }

    @Test
    void blockedCartRedirectsBackToCart() throws Exception {
        mockMvc.perform(get("/checkout").session(sessionFor(ALICE)))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/cart"))
               .andExpect(flash().attribute("error", containsString("请先处理")));
    }

    @Test
    void blockedCartReturnsErrorToCheckout() throws Exception {
        // alice 的种子里有下架商品，直接下单应被打回
        mockMvc.perform(post("/orders")
                                .session(sessionFor(ALICE))
                                .param("receiverName", "林同学")
                                .param("receiverPhone", "13800001111")
                                .param("receiverAddress", "湖南省长沙市岳麓区某路 1 号"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/checkout"))
               .andExpect(flash().attribute("error", containsString("请先处理")));
    }

    @Test
    void successfulOrderRedirectsToDetail() throws Exception {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 1);
        cartService.addItem(ALICE, 22L, 2);

        mockMvc.perform(post("/orders")
                                .session(sessionFor(ALICE))
                                .param("receiverName", "林同学")
                                .param("receiverPhone", "13800001111")
                                .param("receiverAddress", "湖南省长沙市岳麓区某路 1 号"))
               .andExpect(status().is3xxRedirection())
               .andExpect(header().string("Location", matchesPattern("/orders/\\d+")))
               .andExpect(flash().attributeExists("notice"));
    }

    @Test
    void orderDetailIsOnlyVisibleToOwner() throws Exception {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 1);
        String location = mockMvc.perform(post("/orders")
                                                   .session(sessionFor(ALICE))
                                                   .param("receiverName", "林同学")
                                                   .param("receiverPhone", "13800001111")
                                                   .param("receiverAddress", "湖南省长沙市岳麓区某路 1 号"))
                                 .andReturn().getResponse().getRedirectedUrl();
        Long orderId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(get("/orders/" + orderId).session(sessionFor(ALICE)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("订单")));

        mockMvc.perform(get("/orders/" + orderId).session(sessionFor(BOB)))
               .andExpect(status().isNotFound());
    }

    private MockHttpSession sessionFor(Long userId) {
        String username = userId.equals(ALICE) ? "alice" : "bob";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSession.KEY, new LoginUser(userId, username, "林同学", "CUSTOMER"));
        return session;
    }

    private void clearCart(Long userId) {
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }
}
