package com.education.amenity.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "https://trix-mart-upload-1.vercel.app")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private S3Service s3Service; // For generating pre-signed download URLs

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }


    @PostMapping("/register")
    public ResponseEntity<Student> registerStudent(@RequestBody Student student) {
        Student savedStudent = studentRepository.save(student);
        return ResponseEntity.ok(savedStudent);
    }

    @PostMapping("/upload-id")
    public ResponseEntity<String> uploadStudentId(
            @RequestParam("studentId") Long studentId,
            @RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();

            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest()
                        .body("Invalid file type! Only image or PDF files are allowed.");
            }
            studentService.saveStudentFile(file, studentId);
            return ResponseEntity.ok("ID Card uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload ID Card: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/{studentId}/id-card")
    public ResponseEntity<String> getStudentIdCard(@PathVariable Long studentId) {
        try {
            // Directly fetch the student object
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Check if the student has a file
            if (student.getFileName() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID Card not found.");
            }

            // Generate presigned URL for the file
            String url = s3Service.generatePresignedDownloadUrl(student.getFileName());
            return ResponseEntity.ok(url);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating download link: " + e.getMessage());
        }
    }


    // Handle incoming WhatsApp webhook messages to create student records
    @PostMapping("/whatsapp-webhook")
    public ResponseEntity<String> handleWhatsAppMessage(@RequestBody WhatsappMessage message) {
        // Only require basic fields; file details are managed via separate file upload
        if (message.getStudentId() == null || message.getStudentName() == null || message.getBusinessName() == null) {
            return ResponseEntity.badRequest()
                    .body("Invalid data: studentId, studentName, and businessName are required.");
        }

        try {
            Student student = new Student();
            student.setStudentId(message.getStudentId());
            student.setStudentName(message.getStudentName());
            student.setBusinessName(message.getBusinessName());
            student.setBusinessType(message.getBusinessType());
            student.setSubscriptionType(message.getSubscriptionType());
            student.setPhoneNumber(message.getPhoneNumber());

            studentRepository.save(student);
            return ResponseEntity.ok("Student created successfully from WhatsApp message!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing WhatsApp message: " + e.getMessage());
        }
    }

    @PostMapping("/get-presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUploadUrl(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        if(studentId == null) {
            return ResponseEntity.badRequest().body(null);
        }
        // Generate file key
        String fileKey = "students/" + studentId + "/" + UUID.randomUUID() + "_id_upload";
        try {
            // Use the S3Service fields (bucket, region, and presigner) to build the presigned URL
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Service.getBucketName())
                    .key(fileKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .putObjectRequest(putRequest)
                    .signatureDuration(Duration.ofMinutes(10))
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Service.getPresigner().presignPutObject(presignRequest);
            Map<String, String> response = new HashMap<>();
            response.put("uploadUrl", presignedRequest.url().toString());
            response.put("fileKey", fileKey);
            return ResponseEntity.ok(response);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/update-file")
    public ResponseEntity<String> updateFile(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        String fileKey = request.get("fileKey");
        if(studentId == null || fileKey == null) {
            return ResponseEntity.badRequest().body("Missing studentId or fileKey");
        }
        try {
            Student student = studentRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new Exception("Student not found"));
            // Generate a presigned download URL using your existing S3 logic
            String downloadUrl = s3Service.generatePresignedDownloadUrl(fileKey);
            student.setFileName(downloadUrl);
            studentRepository.save(student);
            return ResponseEntity.ok("Student record updated with file URL");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating student record: " + e.getMessage());
        }
    }


}
