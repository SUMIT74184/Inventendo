package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.exception.InsufficientInventoryException;
import org.example.exception.OrderNotFoundException;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.repository.OrderRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventProducer eventProducer;

    public OrderResponse createOrder(OrderRequest request){
        log.info("Creating order for customer: {}",request.getCustomerId());



        List<OrderItem> orderItems = request.getItems().stream()
                .map(item->processOrderItem(item))
                .collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .build();


        orderItems.forEach((item->{
            item.setOrder(order);
            order.getOrderItems().add(item);
        }));

        Order savedOrder = orderRepository.save(order);

        try{
            reservedInventory(savedOrder);
            savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(savedOrder);
            eventProducer.publishOrderCreatedEvent(savedOrder.getId(),savedOrder.getOrderNumber());
        }catch (InsufficientInventoryException e){
            savedOrder.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(savedOrder);
            throw e;
        }
        return mapToResponse(savedOrder);
    }

    private OrderItem processOrderItem(OrderItemRequest itemRequest){
        InventoryResponse inventory = inventoryClient.getInventory(itemRequest.getProductId());

        if(!inventory.isInStock()  || inventory.getAvailableQuantity() < itemRequest.getQuantity()){
            throw new InsufficientInventoryException(
            "Insufficient inventory for product: " + inventory.getProductName()
            );
        }

        BigDecimal totalPrice = inventory.getPrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

        return OrderItem.builder()
                .productId(itemRequest.getProductId())
                .productName(inventory.getProductName())
                .productSku(inventory.getSku())
                .quantity(itemRequest.getQuantity())
                .unitPrice(inventory.getPrice())
                .totalPrice(totalPrice)
                .build();


    }

    private void reservedInventory(Order order){
        for(OrderItem item : order.getOrderItems()){
            boolean reserved = inventoryClient.reserveInventory(
                    item.getProductId(),
                    item.getQuantity()
            );

            if(!reserved){
                releaseReservedInventory(order);
                throw new InsufficientInventoryException(
                        "Failed to reserve inventory for product: " + item.getProductName());
            }

        }
    }


    private void releaseReservedInventory(Order order){
        order.getOrderItems().forEach(item->{
            try{
                inventoryClient.releaseInventory(item.getProductId(),item.getQuantity());
            }catch (Exception e){
                log.error("Error releasing inventory for product:{}",item.getProductId(),e);
            }
        });
    }



    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id){
        Order order = orderRepository.findById(id)
                .orElseThrow(()-> new OrderNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Cacheable(value = "orders", key = "#orderNumber")
    public OrderResponse getOrderByNumber(String orderNumber){
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(()-> new OrderNotFoundException("Order not found:" + orderNumber));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomerId(Long customerId){
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Cacheable(value = "order", key = "#id")
    public OrderResponse updateOrderStatus(Long id, String status){
        Order order = orderRepository.findById(id)
                .orElseThrow(()-> new OrderNotFoundException("Order not found with id: " + id));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        eventProducer.publishOrderStatusChangedEvent(
                updatedOrder.getId(),
                updatedOrder.getOrderNumber(),
                status
        );

        return mapToResponse(updatedOrder);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    public void cancelOrder(Long id) throws IllegalAccessException {
        Order order = orderRepository.findById(id)
                .orElseThrow(()-> new OrderNotFoundException("Order not found with id: " + id));

        if(order.getStatus() == Order.OrderStatus.SHIPPED ||
        order.getStatus() == Order.OrderStatus.DELIVERED){
            throw new IllegalAccessException("Cannot cancel order in status: " + order.getStatus());

        }
        releaseReservedInventory(order);
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        eventProducer.publishOrderStatusChangedEvent(
                order.getId(),
                order.getOrderNumber(),
                "CANCELLED"
        );

    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();

    }

    private OrderResponse mapToResponse(Order order){
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getOrderItems().stream()
                        .map(this::mapItemToResponse)
                        .collect(Collectors.toList())
                )
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item){
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();

    }




}
