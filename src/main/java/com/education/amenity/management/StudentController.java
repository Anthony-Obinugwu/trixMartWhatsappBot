package com.education.amenity.management;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerStudent(@RequestBody Student student) {
        Student savedStudent = studentRepository.save(student);
        return ResponseEntity.ok("Student registered successfully: " + savedStudent.getStudentName());
    }

    @PostMapping("/test-save-student")
    public ResponseEntity<String> testSaveStudent() {
        // Create and populate a test Student object with valid data
        Student testStudent = new Student();
        testStudent.setStudentId("TEST12346"); // Unique ID
        testStudent.setStudentName("Test Name");

        // Provide required non-null values
        testStudent.setFileName("test_file.txt");
        testStudent.setFilePath("/home/uploads/test_file.txt");
        testStudent.setFileType("text/plain");

        testStudent.setBusinessName("Test Business");
        testStudent.setBusinessType("eCommerce");
        testStudent.setSubscriptionType("Standard");
        testStudent.setPhoneNumber("1234567890");

        // Save to the database
        studentRepository.saveAndFlush(testStudent);
        System.out.println("Test student saved successfully!");

        return ResponseEntity.ok("Test student saved successfully!");
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(HttpServletRequest request) {
        try {
            InputStream fileInputStream = request.getInputStream();
            String studentName = request.getParameter("studentName");

            // Process the file contents here
            if (studentName == null || studentName.isEmpty()) {
                return ResponseEntity.badRequest().body("Student name is required.");
            }

            // Example: Just count the bytes as a sample file processing
            int byteCount = fileInputStream.readAllBytes().length;
            return ResponseEntity.ok("File processed successfully for student: " + studentName + ", Size: " + byteCount + " bytes.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing file: " + e.getMessage());
        }
    }



    @PostMapping("/whatsapp-webhook")
    public ResponseEntity<String> handleWhatsAppMessage(@RequestBody WhatsappMessage message) {
        // Validate and ensure required fields
        if (message.getStudentId() == null || message.getStudentName() == null || message.getBusinessName() == null) {
            return ResponseEntity.badRequest().body("Invalid data: studentId, studentName, and businessName are required.");
        }

        try {
            // Create a new Student object and map the incoming WhatsappMessage fields
            Student student = new Student();
            student.setStudentId(message.getStudentId());
            student.setStudentName(message.getStudentName());
            student.setBusinessName(message.getBusinessName());

            // Handle optional fields with safe defaults
            student.setBusinessType(Optional.ofNullable(message.getBusinessType()).orElse("Default Business Type"));
            student.setSubscriptionType(Optional.ofNullable(message.getSubscriptionType()).orElse("Free"));
            student.setPhoneNumber(Optional.ofNullable(message.getPhoneNumber()).orElse("Unknown"));

            // Handle file information (optional)
            student.setFileName(Optional.ofNullable(message.getFileName()).orElse("default_file_name.txt"));
            student.setFilePath(Optional.ofNullable(message.getFilePath()).orElse("/default/path/"));
            student.setFileType(Optional.ofNullable(message.getFileType()).orElse("unknown"));

            // Save the student information into the database
            studentRepository.saveAndFlush(student);

            // Respond with a success message
            return ResponseEntity.ok("Student created successfully from WhatsApp message!");
        } catch (Exception e) {
            // Log the error and provide feedback to the client
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred while processing the WhatsApp message.");
        }
    }
}
