package org.csu.mypetstore.service;

/** 支付 / 取消这类"订单状态动作"的结果：成不成功 + 给用户看的一句话。 */
public record OrderAction(boolean success, String message) {

    public static OrderAction ok(String message) {
        return new OrderAction(true, message);
    }

    public static OrderAction fail(String message) {
        return new OrderAction(false, message);
    }
}
