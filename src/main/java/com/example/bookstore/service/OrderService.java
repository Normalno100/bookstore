package com.example.bookstore.service;

import com.example.bookstore.dto.CartItem;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Order;
import com.example.bookstore.model.OrderItem;
import com.example.bookstore.model.User;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    public OrderService(OrderRepository orderRepository, BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public Order createOrder(User user, List<CartItem> cartItems,
                             String deliveryAddress, String phone, String email) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }

        Order order = Order.builder()
                .user(user)
                .deliveryAddress(deliveryAddress)
                .customerPhone(phone)
                .customerEmail(email)
                .status(Order.OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Book book = bookRepository.findById(cartItem.getBookId())
                    .orElseThrow(() -> new RuntimeException("Книга не найдена: " + cartItem.getBookId()));

            // Проверка наличия на складе
            if (book.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Недостаточно товара на складе: " + book.getTitle());
            }

            OrderItem orderItem = OrderItem.builder()
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .priceAtOrder(book.getPrice())
                    .build();

            order.addItem(orderItem);
            total = total.add(orderItem.getSubtotal());

            // Уменьшаем количество на складе
            book.setStock(book.getStock() - cartItem.getQuantity());
            bookRepository.save(book);
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Недостаточно прав");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Можно отменить только заказы в статусе 'Ожидает обработки'");
        }

        // Возвращаем товары на склад
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStock(book.getStock() + item.getQuantity());
            bookRepository.save(book);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}