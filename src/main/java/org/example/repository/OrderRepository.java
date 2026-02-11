package org.example.repository;

import org.example.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByCreatedAtBetween(LocalDateTime startDate,LocalDateTime endDate);

    List<Order> findByCustomerIdAndStatus(Long customerId,Order.OrderStatus status);

    boolean existByOrderNumber(String orderNumber);
}
