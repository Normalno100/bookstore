package com.example.bookstore.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String title;
    private String author;
    private String genre;
    @Column(length = 2000)
    private String description;


    private String isbn;
    private Double price;
    private Integer stock;
}
