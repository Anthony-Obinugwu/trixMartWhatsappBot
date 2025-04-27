package com.education.amenity.management;

import lombok.*;

@Data
public class Student {
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