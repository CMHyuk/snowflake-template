package com.example.snowflake.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void snowFlakeTest() {
        Long orderId1 = orderService.save();
        System.out.println("orderId1 = " + orderId1);

        Long orderId2 = orderService.save();
        System.out.println("orderId2 = " + orderId2);
    }

}