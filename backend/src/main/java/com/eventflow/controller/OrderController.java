package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateOrderRequest;
import com.eventflow.dto.OrderResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final TicketService ticketService;

    public OrderController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        OrderResponse order = ticketService.createOrder(request, currentUser.getId());
        return new ResponseEntity<>(ApiResponse.success("Order confirmed and tickets generated successfully", order), HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<OrderResponse> orders = ticketService.getMyOrders(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
