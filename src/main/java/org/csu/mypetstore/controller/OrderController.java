package org.csu.mypetstore.controller;

import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.dto.CartSnapshot;
import org.csu.mypetstore.dto.Receiver;
import org.csu.mypetstore.entity.SalesOrder;
import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.service.CartService;
import org.csu.mypetstore.service.OrderAction;
import org.csu.mypetstore.service.OrderException;
import org.csu.mypetstore.service.OrderService;
import org.csu.mypetstore.service.UserService;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 结算与订单查询。写操作只有"下单"一个，全部细节在 OrderService 的事务里。
 */
@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        CartSnapshot cart = cartService.getCart(user.id());
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        // 有下架 / 售罄 / 超库存的条目：回购物车处理，不让用户白填一遍收货信息
        if (cart.hasBlockedLine()) {
            redirect.addFlashAttribute("error", "购物车里有已下架、已售罄或超出库存的商品，请先处理");
            return "redirect:/cart";
        }

        String phone = userService.findByUsername(user.username())
                                  .map(User::getPhone)
                                  .orElse("");

        model.addAttribute("cart", cart);
        model.addAttribute("profileName", user.displayName());
        model.addAttribute("profilePhone", phone == null ? "" : phone);
        return "checkout";
    }

    @PostMapping("/orders")
    public String place(@RequestParam(required = false) String receiverName,
                        @RequestParam(required = false) String receiverPhone,
                        @RequestParam(required = false) String receiverAddress,
                        HttpSession session,
                        RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        try {
            Long orderId = orderService.placeOrder(user.id(),
                    new Receiver(receiverName, receiverPhone, receiverAddress));
            redirect.addFlashAttribute("notice", "订单已创建，请尽快完成支付");
            return "redirect:/orders/" + orderId;
        } catch (OrderException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        OrderAction action = orderService.pay(user.id(), id);
        redirect.addFlashAttribute(action.success() ? "notice" : "error", action.message());
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        LoginUser user = LoginSession.get(session);
        OrderAction action = orderService.cancel(user.id(), id);
        redirect.addFlashAttribute(action.success() ? "notice" : "error", action.message());
        return "redirect:/orders/" + id;
    }

    @GetMapping("/orders")
    public String list(HttpSession session, Model model) {
        LoginUser user = LoginSession.get(session);
        model.addAttribute("orders", orderService.listOrders(user.id()));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        LoginUser user = LoginSession.get(session);
        SalesOrder order = orderService.findOwnOrder(user.id(), id)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("order", order);
        model.addAttribute("statusLabel", OrderService.statusLabel(order.getStatus()));
        model.addAttribute("items", orderService.findOrderItems(order.getId()));
        return "order";
    }
}
