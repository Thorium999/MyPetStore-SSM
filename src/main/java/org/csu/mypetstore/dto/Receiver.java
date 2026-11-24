package org.csu.mypetstore.dto;

/** 结算页提交的收货信息，已 trim 后用于写订单快照。 */
public record Receiver(String name, String phone, String address) {
}
