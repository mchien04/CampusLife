package vn.campuslife.model;

import lombok.Data;

@Data
public class EmailAttachmentResponse {
    private Long id;
    private String fileName;
    private String fileUrl; // URL để download
    private Long fileSize;
    private String contentType;
}

