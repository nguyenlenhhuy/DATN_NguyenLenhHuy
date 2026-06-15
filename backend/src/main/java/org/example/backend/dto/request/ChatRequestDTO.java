package org.example.backend.dto.request;

public class ChatRequestDTO {
    private String message;
    private String userRole;     // GUEST | CUSTOMER | STAFF | ADMIN
    private String imageBase64;  // Base64 của ảnh đính kèm (nullable)
    private String imageMimeType; // "image/jpeg", "image/png", v.v. (nullable)

    public ChatRequestDTO() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getImageMimeType() { return imageMimeType; }
    public void setImageMimeType(String imageMimeType) { this.imageMimeType = imageMimeType; }

    public boolean hasImage() {
        return imageBase64 != null && !imageBase64.isBlank();
    }
}
