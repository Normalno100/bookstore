package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookService bookService;
    private final FileStorageService fileStorageService;

    public AdminController(BookService bookService, FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.findAll());
        return "admin/book-list";
    }

    @GetMapping
    public String adminPanel() {
        return "redirect:/admin/books";
    }

    @GetMapping("/books/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "admin/book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id);
        if (book == null) {
            return "redirect:/admin";
        }
        model.addAttribute("book", book);
        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(
            @RequestParam(required = false) Long id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String genre,
            @RequestParam String description,
            @RequestParam String isbn,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(required = false) MultipartFile image,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Book book;
            String oldImagePath = null;

            if (id != null) {
                // Редактирование существующей книги
                book = bookService.findById(id);
                if (book == null) {
                    redirectAttributes.addFlashAttribute("error", "Книга не найдена");
                    return "redirect:/admin";
                }
                oldImagePath = book.getImagePath();
            } else {
                // Создание новой книги
                book = new Book();
            }

            // Обновляем поля
            book.setTitle(title);
            book.setAuthor(author);
            book.setGenre(genre);
            book.setDescription(description);
            book.setIsbn(isbn);
            book.setPrice(price);
            book.setStock(stock);

            // Обработка изображения
            if (image != null && !image.isEmpty()) {
                // Удаляем старое изображение, если оно было
                if (oldImagePath != null) {
                    fileStorageService.deleteFile(oldImagePath);
                }
                // Сохраняем новое изображение
                String imagePath = fileStorageService.storeFile(image);
                book.setImagePath(imagePath);
            }

            bookService.save(book);

            redirectAttributes.addFlashAttribute("success",
                    id != null ? "Книга успешно обновлена" : "Книга успешно добавлена");
            return "redirect:/admin";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/admin/books/" + (id != null ? "edit/" + id : "new");
        }
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.findById(id);
            if (book == null) {
                redirectAttributes.addFlashAttribute("error", "Книга не найдена");
                return "redirect:/admin";
            }

            // Удаляем изображение, если оно есть
            if (book.getImagePath() != null) {
                fileStorageService.deleteFile(book.getImagePath());
            }

            bookService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Книга успешно удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}