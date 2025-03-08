package com.education.amenity.management;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/pending")
    public List<Payment> getPendingPayments() {
        return paymentService.getPendingPayments();
    }

    @PostMapping("/verify/{id}")
    public Payment verifyPayment(@PathVariable Long id, @RequestBody Admin admin) {
        return paymentService.verifyPayment(id, admin);
    }
}

