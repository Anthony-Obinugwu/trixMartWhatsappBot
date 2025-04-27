package com.education.amenity.management;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long studentId) {
        super("Student not found with ID: " + studentId);
    }
}
