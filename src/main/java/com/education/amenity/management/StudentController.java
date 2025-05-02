package com.education.amenity.management;

import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = {
        "https://trix-mart-upload-vercel-tawny.vercel.app",
        "http://localhost:3000",
        "http://localhost:8080",
        "http://147.182.193.125"
})
@Validated
public class StudentController {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @Autowired
    private BotNotificationService botNotificationService;

    @PostMapping("/upload-id")
    public ResponseEntity<?> uploadStudentId(
            @RequestParam("studentId") String studentId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();

            if (originalFilename != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp", "avif", "svg", "pdf");
                if (!allowedExtensions.contains(extension)) {
                    return ResponseEntity.badRequest()
                            .body("Invalid file extension! Allowed extensions: " + allowedExtensions);
                }
            }

            if (contentType == null || !contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body("Invalid file type! Only image or PDF files are allowed.");
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body("File size exceeds 10MB limit");
            }

            // Upload to Firebase Storage
            String fileUrl = firebaseStorageService.uploadFile(file);

            // Update Firestore document
            Firestore firestore = firebaseService.getFirestore();
            Map<String, Object> updates = new HashMap<>();
            updates.put("fileUrl", fileUrl);
            updates.put("fileType", file.getContentType());
            updates.put("fileSize", file.getSize());
            updates.put("uploadStatus", "COMPLETED");

            firestore.collection("students")
                    .document(studentId)
                    .update(updates)
                    .get();

            // Notify bot
            botNotificationService.notifyBot(studentId);

            return ResponseEntity.ok().build();

        } catch (IOException | InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process request: " + e.getMessage());
        }
    }

    @PostMapping("/whatsapp-webhook")
    public ResponseEntity<String> handleWhatsAppMessage(@RequestBody WhatsappMessage message) {
        try {
            Firestore firestore = firebaseService.getFirestore();
            Map<String, Object> student = new HashMap<>();
            student.put("studentId", message.getStudentId());
            student.put("studentName", message.getStudentName());
            student.put("businessName", message.getBusinessName());
            student.put("businessType", message.getBusinessType());
            student.put("subscriptionType", message.getSubscriptionType());
            student.put("phoneNumber", message.getPhoneNumber());
            student.put("uploadStatus", "PENDING");

            firestore.collection("students")
                    .document(message.getStudentId().toString())
                    .set(student)
                    .get();

            return ResponseEntity.ok("Student created successfully from WhatsApp message!");
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing WhatsApp message: " + e.getMessage());
        }
    }

    @ControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationExceptions(
                MethodArgumentNotValidException ex) {
            Map<String, String> errors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
            return ResponseEntity.badRequest().body(errors);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", ex.getMessage()));
        }
    }
}