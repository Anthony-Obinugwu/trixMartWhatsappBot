package com.education.amenity.management;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findById(Long id);
    Optional<Student> findByStudentName(String studentName);
    Optional<Student> findByPhoneNumber(String phoneNumber);


}
