package com.example.bookstore.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath = Paths.get("uploads/books");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для загрузки файлов", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Получаем оригинальное имя файла
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Проверяем расширение файла
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        // Разрешенные расширения
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new RuntimeException("Недопустимый формат файла. Разрешены: jpg, jpeg, png, gif, webp");
        }

        try {
            // Проверяем на недопустимые символы
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Недопустимое имя файла: " + originalFilename);
            }

            // Генерируем уникальное имя файла
            String filename = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.uploadPath.resolve(filename);

            // Копируем файл в целевую директорию
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/books/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл " + originalFilename, e);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            // Извлекаем имя файла из пути
            String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
            Path file = uploadPath.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Логируем ошибку, но не прерываем выполнение
            System.err.println("Не удалось удалить файл: " + filePath);
        }
    }
}