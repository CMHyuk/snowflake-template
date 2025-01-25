package com.example.snowflake.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public Long save() {
        Order order = new Order("테스트 상품", 100);
        return orderRepository.save(order).getId();
    }

}
