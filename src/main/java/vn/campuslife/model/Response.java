package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private boolean status;
    private String message;
    private Object body;
    public static Response success(String message, Object data) {
        return new Response(true, message, data);
    }

    public static Response success(String message) {
        return new Response(true, message, null);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }
}