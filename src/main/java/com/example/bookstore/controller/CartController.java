package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.User;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.CartService;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final BookService bookService;
    private final OrderService orderService;
    private final UserService userService;

    public CartController(CartService cartService, BookService bookService,
                          OrderService orderService, UserService userService) {
        this.cartService = cartService;
        this.bookService = bookService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getTotal());
        return "cart";
    }

    @PostMapping("/add/{bookId}")
    public String addToCart(@PathVariable Long bookId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            RedirectAttributes redirectAttributes) {
        Book book = bookService.findById(bookId);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Книга не найдена");
            return "redirect:/books";
        }

        if (book.getStock() < quantity) {
            redirectAttributes.addFlashAttribute("error", "Недостаточно товара на складе");
            return "redirect:/books/" + bookId;
        }

        cartService.addItem(book, quantity);
        redirectAttributes.addFlashAttribute("success", "Товар добавлен в корзину");
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/remove/{bookId}")
    public String removeFromCart(@PathVariable Long bookId) {
        cartService.removeItem(bookId);
        return "redirect:/cart";
    }

    @PostMapping("/update/{bookId}")
    public String updateQuantity(@PathVariable Long bookId,
                                 @RequestParam Integer quantity) {
        cartService.updateQuantity(bookId, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutForm(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        if (cartService.isEmpty()) {
            return "redirect:/cart";
        }

        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getTotal());
        model.addAttribute("user", user);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String deliveryAddress,
                                  @RequestParam String phone,
                                  @RequestParam String email,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            Order order = orderService.createOrder(
                    user,
                    cartService.getItems(),
                    deliveryAddress,
                    phone,
                    email
            );

            cartService.clear();
            redirectAttributes.addFlashAttribute("success",
                    "Заказ №" + order.getId() + " успешно оформлен!");
            return "redirect:/orders";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart/checkout";
        }
    }
}