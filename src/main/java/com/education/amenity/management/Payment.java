package com.education.amenity.management;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "verified_by")
    private Admin verifiedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum PaymentType {
        PREMIUM, NORMAL
    }

    public enum PaymentStatus {
        PENDING, VERIFIED, REJECTED
    }
}

