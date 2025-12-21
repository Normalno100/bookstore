package com.example.bookstore.controller;

import com.example.bookstore.model.Order;
import com.example.bookstore.model.User;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public String listOrders(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("orders", orderService.getUserOrders(user));
        return "orders";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/orders";
        }

        User user = userService.findByUsername(authentication.getName());
        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            orderService.cancelOrder(id, user);
            redirectAttributes.addFlashAttribute("success", "Заказ успешно отменён");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + id;
    }
}