package com.education.amenity.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    public Student saveStudentFile(InputStream fileInputStream, String studentName, String fileName, String contentType) throws Exception {
        // Generate file path with unique name
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        Path path = Paths.get(UPLOAD_DIR, uniqueFileName);

        // Create directories if not present
        Files.createDirectories(path.getParent());

        // Write file data from input stream
        Files.copy(fileInputStream, path, StandardCopyOption.REPLACE_EXISTING);

        // Handle student information
        Optional<Student> existingStudent = studentRepository.findByStudentName(studentName);
        Student student;

        if (existingStudent.isPresent()) {
            student = existingStudent.get();
        } else {
            student = new Student();
            student.setStudentName(studentName);
        }

        // Update student file information
        student.setFileName(uniqueFileName);
        student.setFilePath(path.toString());
        student.setFileType(contentType);

        // Save student record
        return studentRepository.save(student);
    }


    public void saveStudent(Student student) {
        studentRepository.save(student);
    }


    public boolean deleteDocument(String fileName) throws Exception {
        Path path = Paths.get(UPLOAD_DIR, fileName);

        if (Files.exists(path)) {
            Files.delete(path);
            return true;
        }
        return false;
    }

    public boolean copyDocument(String sourceFileName, String destinationFileName) throws Exception {
        Path sourcePath = Paths.get(UPLOAD_DIR, sourceFileName);
        Path destinationPath = Paths.get(UPLOAD_DIR, destinationFileName);

        if (Files.exists(sourcePath)) {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        return false;
    }
}
