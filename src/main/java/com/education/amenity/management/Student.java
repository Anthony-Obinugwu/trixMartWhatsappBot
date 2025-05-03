package com.education.amenity.management;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    @Column
    private String studentName;

    @Column
    private String businessName;

    @Column
    private String businessType;

    @Column
    private String subscriptionType;

    @Column
    private String phoneNumber;
}