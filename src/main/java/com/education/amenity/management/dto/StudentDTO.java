
package com.education.amenity.management.dto;

import lombok.Data;

@Data
public class StudentDTO {
    private Long studentId;
    private String studentName;
    private String businessName;
    private String businessType;
    private String subscriptionType;
    private String phoneNumber;
    private String fileUrl;      // Firebase URL
    private String fileType;    // e.g., "image/jpeg"
    private Long fileSize;      // in bytes
}