package org.csu.mypetstore.controller;

import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.service.CartService;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 购物车。所有路径都被 LoginInterceptor 保护，这里可以放心直接取 Session 用户。
 * 写操作一律 POST + 重定向（PRG），刷新页面不会重复提交。
 */
@Controller
public class CartController {

    private static final String CART_VIEW = "cart";
    private static final String CART_URL = "redirect:/cart";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        LoginUser user = LoginSession.get(session);
        model.addAttribute("cart", cartService.getCart(user.id()));
        return CART_VIEW;
    }

    @PostMapping("/cart")
    public String add(@RequestParam Long productId,
                      @RequestParam(required = false) Integer quantity,
                      HttpSession session,
                      RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        redirect.addFlashAttribute("notice", cartService.addItem(user.id(), productId, safeQuantity(quantity)));
        return CART_URL;
    }

    @PostMapping("/cart/{itemId}/quantity")
    public String updateQuantity(@PathVariable Long itemId,
                                 @RequestParam(required = false) Integer quantity,
                                 HttpSession session,
                                 RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        redirect.addFlashAttribute("notice",
                cartService.updateQuantity(user.id(), itemId, safeQuantity(quantity)));
        return CART_URL;
    }

    @PostMapping("/cart/{itemId}/delete")
    public String remove(@PathVariable Long itemId,
                         HttpSession session,
                         RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        boolean removed = cartService.removeItem(user.id(), itemId);
        redirect.addFlashAttribute("notice", removed ? "已移出购物车" : "该条目已不存在");
        return CART_URL;
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 1 : quantity;
    }
}
