package com.education.amenity.management;

import com.google.cloud.firestore.Firestore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
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
        "http://localhost:3000"
})
@Validated
public class StudentController {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @Autowired
    private BotNotificationService botNotificationService;

    @Data
    public static class FileUploadResponse {
        private String fileUrl;
        private String fileName;
    }

    @PostMapping("/upload-id")
    public ResponseEntity<?> uploadStudentId(
            @RequestParam("studentId") String studentId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file type
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();

            // Extension validation
            if (originalFilename != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp", "avif", "svg", "pdf");
                if (!allowedExtensions.contains(extension)) {
                    return ResponseEntity.badRequest()
                            .body("Invalid file extension! Allowed extensions: " + allowedExtensions);
                }
            }

            // Content type validation
            if (contentType == null || !contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body("Invalid file type! Only image or PDF files are allowed.");
            }

            // File size validation
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body("File size exceeds 10MB limit");
            }

            String fileUrl = firebaseStorageService.uploadFile(file);
            Firestore firestore = firebaseService.getFirestore();

            Map<String, Object> updates = new HashMap<>();
            updates.put("fileUrl", fileUrl);
            updates.put("fileType", file.getContentType());
            updates.put("fileSize", file.getSize());

            firestore.collection("students")
                    .document(studentId)
                    .update(updates)
                    .get();

            botNotificationService.notifyBot(studentId);

            return ResponseEntity.ok(new FileUploadResponse());

        } catch (IOException | InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process request: " + e.getMessage());
        }
    }

    @GetMapping("/{studentId}/id-card")
    public ResponseEntity<String> getStudentIdCard(@PathVariable String studentId) {
        try {
            Firestore firestore = firebaseService.getFirestore();
            Map<String, Object> student = firestore.collection("students")
                    .document(studentId)
                    .get()
                    .get()
                    .getData();

            if (student == null || !student.containsKey("fileUrl")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID Card not found.");
            }

            return ResponseEntity.ok((String) student.get("fileUrl"));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving ID card: " + e.getMessage());
        }
    }

    @PostMapping("/whatsapp-webhook")
    public ResponseEntity<String> handleWhatsAppMessage(@RequestBody WhatsappMessage message) {
        if (message.getStudentId() == null || message.getStudentName() == null || message.getBusinessName() == null) {
            return ResponseEntity.badRequest()
                    .body("Invalid data: studentId, studentName, and businessName are required.");
        }

        try {
            Firestore firestore = firebaseService.getFirestore();
            Map<String, Object> student = new HashMap<>();
            student.put("studentId", message.getStudentId());
            student.put("studentName", message.getStudentName());
            student.put("businessName", message.getBusinessName());
            student.put("businessType", message.getBusinessType());
            student.put("subscriptionType", message.getSubscriptionType());
            student.put("phoneNumber", message.getPhoneNumber());

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

    @GetMapping("/check-id")
    public ResponseEntity<Map<String, Boolean>> checkIdExistsV2(
            @RequestParam String studentId) {
        try {
            boolean exists = firebaseService.getFirestore()
                    .collection("students")
                    .document(studentId)
                    .get()
                    .get()
                    .exists();

            return ResponseEntity.ok(Collections.singletonMap("exists", exists));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("exists", false));
        }
    }

    // Keep the same GlobalExceptionHandler
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