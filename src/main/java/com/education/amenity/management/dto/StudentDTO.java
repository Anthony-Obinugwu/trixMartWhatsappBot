
package com.education.amenity.management.dto;

import lombok.Data;

@Data
public class StudentDTO {
    private String studentId;
    private String studentName;
    private String businessName;
    private String businessType;
    private String subscriptionType;
    private String phoneNumber;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
}