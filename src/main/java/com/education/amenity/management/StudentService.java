package com.education.amenity.management;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private S3Service s3Service;

    @Transactional
    public void saveStudentFile(MultipartFile file, Long studentId) throws Exception {
        // Check if student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));
        try {
            String uploadedFileKey = s3Service.uploadFile(file);
            student.setFileName(uploadedFileKey);
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload the file to S3: " + e.getMessage());
        }
        studentRepository.save(student);
    }


    public Student createStudent(Student student) throws Exception {
        if (studentRepository.findByStudentId(student.getStudentId()).isPresent() ||
        studentRepository.findByPhoneNumber(student.getPhoneNumber()).isPresent()) {
            throw new Exception("Duplicate studentId or phone number");
        }
        return studentRepository.save(student);
    }
}