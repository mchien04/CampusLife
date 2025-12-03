package vn.campuslife.util;

import java.security.SecureRandom;

/**
 * Utility để tạo password ngẫu nhiên
 */
public class PasswordGenerator {
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Tạo password ngẫu nhiên với độ dài từ 8-12 ký tự (chữ và số)
     * @param length Độ dài password (8-12)
     * @return Password ngẫu nhiên
     */
    public static String generatePassword(int length) {
        if (length < 8 || length > 12) {
            length = 10; // Default length
        }
        
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return password.toString();
    }
    
    /**
     * Tạo password ngẫu nhiên với độ dài mặc định (10 ký tự)
     * @return Password ngẫu nhiên
     */
    public static String generatePassword() {
        return generatePassword(10);
    }
}

