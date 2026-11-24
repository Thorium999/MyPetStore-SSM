package org.csu.mypetstore.service;

/** 下单过程中的业务失败：库存不足、购物车有问题、收货信息不合法。会被整单回滚。 */
public class OrderException extends RuntimeException {

    public OrderException(String message) {
        super(message);
    }
}
