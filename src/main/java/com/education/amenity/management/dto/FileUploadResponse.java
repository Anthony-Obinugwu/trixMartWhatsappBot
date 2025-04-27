package com.education.amenity.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
}