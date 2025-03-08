package com.education.amenity.management;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = true) // Allow null values
    private String fileName;

    @Column(nullable = true) // Allow null values
    private String filePath;

    @Column(nullable = true) // Allow null values
    private String fileType;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessType;

    @Column(nullable = false)
    private String subscriptionType;

    @Column(nullable = false)
    private String phoneNumber;
}
