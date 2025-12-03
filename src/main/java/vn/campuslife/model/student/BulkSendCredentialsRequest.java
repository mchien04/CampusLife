package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request để gửi email hàng loạt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSendCredentialsRequest {
    private List<Long> studentIds; // Danh sách student IDs
}

