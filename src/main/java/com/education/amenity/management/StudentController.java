package com.education.amenity.management;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = {"http://147.182.193.125:3000", "http://localhost:3000"})
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PostMapping("/whatsapp-registration")
    public ResponseEntity<Map<String, String>> handleWhatsAppRegistration(
            @Valid @RequestBody WhatsappMessage message) {

        // Check if student already exists
        if (studentRepository.findByStudentId(message.getStudentId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Student ID already registered"));
        }

        Student student = new Student();
        student.setStudentId(message.getStudentId());
        student.setStudentName(message.getStudentName());
        student.setBusinessName(message.getBusinessName());
        student.setBusinessType(message.getBusinessType());
        student.setSubscriptionType(message.getSubscriptionType());
        student.setPhoneNumber(message.getPhoneNumber());

        studentRepository.save(student);

        return ResponseEntity.ok()
                .body(Collections.singletonMap("message", "Student registration completed successfully!"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing request: " + ex.getMessage());
    }
}