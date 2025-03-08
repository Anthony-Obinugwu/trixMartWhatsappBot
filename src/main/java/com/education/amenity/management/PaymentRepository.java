package com.education.amenity.management;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByVerifiedBy(Admin verifiedBy);
}

