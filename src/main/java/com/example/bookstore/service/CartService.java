package com.example.bookstore.service;

import com.example.bookstore.dto.CartItem;
import com.example.bookstore.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@SessionScope
public class CartService {
    private final List<CartItem> items = new ArrayList<>();

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(Book book, Integer quantity) {
        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getBookId().equals(book.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            items.add(CartItem.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .price(book.getPrice())
                    .quantity(quantity)
                    .build());
        }
    }

    public void removeItem(Long bookId) {
        items.removeIf(item -> item.getBookId().equals(bookId));
    }

    public void updateQuantity(Long bookId, Integer quantity) {
        items.stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity <= 0) {
                        removeItem(bookId);
                    } else {
                        item.setQuantity(quantity);
                    }
                });
    }

    public void clear() {
        items.clear();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}