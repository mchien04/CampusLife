package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.entity.Department;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.*;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.StudentAccountManagementService;
import vn.campuslife.service.StudentScoreInitService;
import vn.campuslife.service.UserUniquenessHelper;
import vn.campuslife.util.EmailUtil;
import vn.campuslife.util.ExcelParser;
import vn.campuslife.util.PasswordGenerator;
import vn.campuslife.util.UserSoftDeleteSupport;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAccountManagementServiceImpl implements StudentAccountManagementService {

    private static final Logger logger = LoggerFactory.getLogger(StudentAccountManagementServiceImpl.class);

    private final ExcelParser excelParser;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentAuthorizationService departmentAuthorizationService;
    private final PasswordEncoder passwordEncoder;
    private final StudentScoreInitService studentScoreInitService;
    private final EmailUtil emailUtil;

    @Override
    public Response uploadAndParseExcel(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Response.error("File is required");
            }

            // Validate file extension
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return Response.error("File must be Excel format (.xlsx or .xls)");
            }

            // Parse Excel
            List<ExcelStudentRow> allRows = excelParser.parseStudentExcel(file);

            // Validate rows
            List<ExcelStudentRow> validRows = new ArrayList<>();
            List<ExcelStudentRow> invalidRows = new ArrayList<>();
            Map<Integer, String> errors = new HashMap<>();

            for (int i = 0; i < allRows.size(); i++) {
                ExcelStudentRow row = allRows.get(i);
                int rowNumber = i + 1; // 1-based row number

                // Validate required fields
                List<String> rowErrors = new ArrayList<>();
                if (row.getStudentCode() == null || row.getStudentCode().trim().isEmpty()) {
                    rowErrors.add("Mã số sinh viên không được để trống");
                }
                if (row.getFullName() == null || row.getFullName().trim().isEmpty()) {
                    rowErrors.add("Họ tên không được để trống");
                }
                if (row.getEmail() == null || row.getEmail().trim().isEmpty()) {
                    rowErrors.add("Email không được để trống");
                } else if (!isValidEmail(row.getEmail())) {
                    rowErrors.add("Email không hợp lệ");
                }

                if (rowErrors.isEmpty()) {
                    validRows.add(row);
                } else {
                    invalidRows.add(row);
                    errors.put(rowNumber, String.join(", ", rowErrors));
                }
            }

            UploadExcelResponse response = new UploadExcelResponse();
            response.setTotalRows(allRows.size());
            response.setValidRows(validRows);
            response.setInvalidRows(invalidRows);
            response.setErrors(errors);

            return Response.success("Excel parsed successfully", response);

        } catch (Exception e) {
            logger.error("Error parsing Excel file: {}", e.getMessage(), e);
            return Response.error("Failed to parse Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response bulkCreateStudents(BulkCreateStudentsRequest request) {
        try {
            if (request.getStudents() == null || request.getStudents().isEmpty()) {
                return Response.error("Danh sách sinh viên không được để trống");
            }

            List<StudentAccountResponse> createdAccounts = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (ExcelStudentRow studentRow : request.getStudents()) {
                try {
                    // Validate
                    if (studentRow.getStudentCode() == null || studentRow.getStudentCode().trim().isEmpty()) {
                        errors.add("Dòng " + studentRow.getStudentCode() + ": Mã số sinh viên không được để trống");
                        continue;
                    }
                    if (studentRow.getEmail() == null || studentRow.getEmail().trim().isEmpty()) {
                        errors.add("Dòng " + studentRow.getStudentCode() + ": Email không được để trống");
                        continue;
                    }
                    if (!isValidEmail(studentRow.getEmail())) {
                        errors.add("Dòng " + studentRow.getStudentCode() + ": Email không hợp lệ");
                        continue;
                    }

                    String studentCode = studentRow.getStudentCode().trim();
                    String email = studentRow.getEmail().trim();
                    String fullName = studentRow.getFullName() != null ? studentRow.getFullName().trim() : "";

                    UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, studentCode, email);
                    UserUniquenessHelper.reclaimDeletedStudentCode(studentRepository, userRepository, studentCode);

                    if (userRepository.existsByUsernameAndIsDeletedFalse(studentCode)) {
                        errors.add("Dòng " + studentCode + ": Mã số sinh viên đã tồn tại");
                        continue;
                    }

                    if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
                        errors.add("Dòng " + studentCode + ": Email đã tồn tại");
                        continue;
                    }

                    if (studentRepository.findByStudentCodeAndIsDeletedFalse(studentCode).isPresent()
                            || studentRepository.findByUserUsernameAndIsDeletedFalse(studentCode).isPresent()) {
                        errors.add("Dòng " + studentCode + ": Mã số sinh viên đã tồn tại");
                        continue;
                    }

                    // Generate password
                    String plainPassword = PasswordGenerator.generatePassword();
                    String hashedPassword = passwordEncoder.encode(plainPassword);

                    // Create User
                    User user = new User();
                    user.setUsername(studentCode);
                    user.setEmail(email);
                    user.setPassword(hashedPassword);
                    user.setRole(Role.STUDENT);
                    user.setActivated(true); // Không cần email confirmation
                    user.setDeleted(false);
                    User savedUser = userRepository.save(user);

                    // Create Student
                    Student student = new Student();
                    student.setUser(savedUser);
                    student.setStudentCode(studentCode);
                    student.setFullName(fullName);
                    student.setDeleted(false);
                    Student savedStudent = studentRepository.save(student);

                    // Initialize scores
                    try {
                        studentScoreInitService.initializeStudentScoresForCurrentSemester(savedStudent);
                    } catch (Exception e) {
                        logger.warn("Failed to initialize scores for student {}: {}", studentCode, e.getMessage());
                        // Continue even if score initialization fails
                    }

                    // Create response
                    StudentAccountResponse accountResponse = new StudentAccountResponse();
                    accountResponse.setUserId(savedUser.getId());
                    accountResponse.setStudentId(savedStudent.getId());
                    accountResponse.setUsername(savedUser.getUsername());
                    accountResponse.setEmail(savedUser.getEmail());
                    accountResponse.setStudentCode(savedStudent.getStudentCode());
                    accountResponse.setFullName(savedStudent.getFullName());
                    accountResponse.setPassword(plainPassword); // Plain password for review
                    accountResponse.setIsActivated(savedUser.isActivated());
                    accountResponse.setEmailSent(false);
                    accountResponse.setLastLogin(savedUser.getLastLogin());
                    accountResponse.setCreatedAt(savedUser.getCreatedAt());
                    fillDepartmentFields(accountResponse, savedStudent);

                    createdAccounts.add(accountResponse);

                } catch (Exception e) {
                    logger.error("Error creating student account for {}: {}", studentRow.getStudentCode(),
                            e.getMessage(), e);
                    errors.add("Dòng " + studentRow.getStudentCode() + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("createdAccounts", createdAccounts);
            result.put("errors", errors);
            result.put("successCount", createdAccounts.size());
            result.put("errorCount", errors.size());

            return Response.success(
                    String.format("Created %d accounts successfully. %d errors.", createdAccounts.size(),
                            errors.size()),
                    result);

        } catch (Exception e) {
            logger.error("Error in bulkCreateStudents: {}", e.getMessage(), e);
            return Response.error("Failed to create student accounts: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response createStudent(CreateStudentRequest request) {
        return createStudent(request, null);
    }

    @Override
    @Transactional
    public Response createStudent(CreateStudentRequest request, DepartmentScope scope) {
        if (request == null) {
            return Response.error("Yêu cầu không hợp lệ");
        }

        ExcelStudentRow row = new ExcelStudentRow(request.getStudentCode(), request.getFullName(), request.getEmail());
        BulkCreateStudentsRequest bulkRequest = new BulkCreateStudentsRequest(Collections.singletonList(row));

        Response bulkResponse = bulkCreateStudents(bulkRequest);
        if (!bulkResponse.isStatus()) {
            return bulkResponse;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) bulkResponse.getBody();
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");
        @SuppressWarnings("unchecked")
        List<StudentAccountResponse> createdAccounts = (List<StudentAccountResponse>) result.get("createdAccounts");

        if (!errors.isEmpty()) {
            return Response.error(errors.get(0));
        }
        if (createdAccounts.isEmpty()) {
            return Response.error("Không thể tạo tài khoản");
        }

        StudentAccountResponse account = createdAccounts.get(0);
        if (request.getDepartmentId() != null) {
            try {
                Student student = studentRepository.findByIdAndIsDeletedFalse(account.getStudentId())
                        .orElseThrow(() -> new IllegalArgumentException("Student not found"));
                applyDepartmentToStudent(student, request.getDepartmentId(), scope);
                studentRepository.save(student);
                fillDepartmentFields(account, student);
            } catch (IllegalArgumentException e) {
                return Response.error(e.getMessage());
            }
        }

        return Response.success("Tạo tài khoản sinh viên thành công", account);
    }

    @Override
    @Transactional
    public Response createMultipleStudents(CreateMultipleStudentsRequest request) {
        if (request == null || request.getStudents() == null || request.getStudents().isEmpty()) {
            return Response.error("Danh sách sinh viên không được để trống");
        }
        
        List<ExcelStudentRow> rows = request.getStudents().stream()
                .map(student -> new ExcelStudentRow(student.getStudentCode(), student.getFullName(), student.getEmail()))
                .collect(Collectors.toList());
                
        BulkCreateStudentsRequest bulkRequest = new BulkCreateStudentsRequest(rows);
        return bulkCreateStudents(bulkRequest);
    }

    @Override
    public Response getPendingAccounts() {
        return getPendingAccounts(null);
    }

    @Override
    public Response getPendingAccounts(DepartmentScope scope) {
        try {
            // Lấy sinh viên (chưa bị xóa). Manager chỉ thấy sinh viên thuộc khoa được phân công.
            List<Student> students;
            if (scope != null && scope.manager() && !scope.admin()) {
                students = studentRepository.findAll(DepartmentScopeSpec.student(scope.departmentIds()));
            } else {
                students = studentRepository.findAll().stream()
                        .filter(s -> !s.isDeleted())
                        .collect(Collectors.toList());
            }

            List<StudentAccountResponse> accounts = new ArrayList<>();
            for (Student student : students) {
                User user = student.getUser();
                if (user == null || user.isDeleted()) {
                    continue;
                }

                StudentAccountResponse response = new StudentAccountResponse();
                response.setUserId(user.getId());
                response.setStudentId(student.getId());
                response.setUsername(user.getUsername());
                response.setEmail(user.getEmail());
                response.setStudentCode(student.getStudentCode());
                response.setFullName(student.getFullName());
                response.setPassword(null); // Không hiển thị password sau khi đã tạo
                response.setIsActivated(user.isActivated());
                // ⚠️ LƯU Ý: Logic này KHÔNG CHÍNH XÁC
                // emailSent = true chỉ khi user đã login, nhưng email có thể đã gửi mà user
                // chưa login
                // Frontend nên dùng lastLogin để xác định trạng thái thay vì dựa vào emailSent
                response.setEmailSent(user.getLastLogin() != null);
                response.setLastLogin(user.getLastLogin());
                response.setCreatedAt(user.getCreatedAt());
                fillDepartmentFields(response, student);

                accounts.add(response);
            }

            // Sắp xếp theo thời gian tạo (mới nhất trước)
            accounts.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null)
                    return 0;
                if (a.getCreatedAt() == null)
                    return 1;
                if (b.getCreatedAt() == null)
                    return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            return Response.success("Pending accounts retrieved successfully", accounts);

        } catch (Exception e) {
            logger.error("Error getting pending accounts: {}", e.getMessage(), e);
            return Response.error("Failed to get pending accounts: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateStudentAccount(Long studentId, UpdateStudentAccountRequest request) {
        return updateStudentAccount(studentId, request, null);
    }

    @Override
    @Transactional
    public Response updateStudentAccount(Long studentId, UpdateStudentAccountRequest request, DepartmentScope scope) {
        try {
            Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if (scope != null && scope.manager() && !scope.admin()) {
                if (student.getDepartment() != null) {
                    departmentAuthorizationService.requireStudentAccess(studentId, scope);
                } else if (request.getDepartmentId() != null
                        && !scope.departmentIds().contains(request.getDepartmentId())) {
                    return Response.error("Department is outside your scope");
                }
            }

            User user = student.getUser();
            if (user == null || user.isDeleted()) {
                return Response.error("User not found");
            }

            boolean updated = false;

            // Update username
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                String newUsername = request.getUsername().trim();
                // Check if username already exists (excluding current user)
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, newUsername, null);
                Optional<User> existingUser = userRepository.findByUsernameAndIsDeletedFalse(newUsername);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    return Response.error("Username already exists");
                }
                user.setUsername(newUsername);
                updated = true;
            }

            // Update email
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                String newEmail = request.getEmail().trim();
                if (!isValidEmail(newEmail)) {
                    return Response.error("Invalid email format");
                }
                // Check if email already exists (excluding current user)
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, null, newEmail);
                Optional<User> existingUser = userRepository.findByEmailAndIsDeletedFalse(newEmail);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    return Response.error("Email already exists");
                }
                user.setEmail(newEmail);
                updated = true;
            }

            // Update studentCode
            if (request.getStudentCode() != null && !request.getStudentCode().trim().isEmpty()) {
                String newStudentCode = request.getStudentCode().trim();
                // Check if studentCode already exists (excluding current student)
                UserUniquenessHelper.reclaimDeletedStudentCode(studentRepository, userRepository, newStudentCode);
                Optional<Student> existingStudent = studentRepository
                        .findByStudentCodeAndIsDeletedFalse(newStudentCode);
                if (existingStudent.isEmpty()) {
                    existingStudent = studentRepository.findByUserUsernameAndIsDeletedFalse(newStudentCode);
                }
                if (existingStudent.isPresent() && !existingStudent.get().getId().equals(student.getId())) {
                    return Response.error("Student code already exists");
                }
                student.setStudentCode(newStudentCode);
                updated = true;
            }

            // Update fullName
            if (request.getFullName() != null) {
                student.setFullName(request.getFullName().trim());
                updated = true;
            }

            if (request.getDepartmentId() != null) {
                applyDepartmentToStudent(student, request.getDepartmentId(), scope);
                updated = true;
            }

            if (!updated) {
                return Response.error("No fields to update");
            }

            userRepository.save(user);
            studentRepository.save(student);

            StudentAccountResponse response = new StudentAccountResponse();
            response.setUserId(user.getId());
            response.setStudentId(student.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setStudentCode(student.getStudentCode());
            response.setFullName(student.getFullName());
            response.setPassword(null);
            response.setIsActivated(user.isActivated());
            // ⚠️ LƯU Ý: Logic này KHÔNG CHÍNH XÁC
            // emailSent = true chỉ khi user đã login, nhưng email có thể đã gửi mà user
            // chưa login
            // Frontend nên dùng lastLogin để xác định trạng thái thay vì dựa vào emailSent
            response.setEmailSent(user.getLastLogin() != null);
            response.setLastLogin(user.getLastLogin());
            response.setCreatedAt(user.getCreatedAt());
            fillDepartmentFields(response, student);

            return Response.success("Student account updated successfully", response);

        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating student account: {}", e.getMessage(), e);
            return Response.error("Failed to update student account: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response deleteStudentAccount(Long studentId) {
        return deleteStudentAccount(studentId, null);
    }

    @Override
    @Transactional
    public Response deleteStudentAccount(Long studentId, DepartmentScope scope) {
        try {
            Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }

            // Soft delete
            UserSoftDeleteSupport.softDelete(student);
            studentRepository.save(student);
            if (student.getUser() != null) {
                userRepository.save(student.getUser());
            }

            return Response.success("Student account deleted successfully", null);

        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting student account: {}", e.getMessage(), e);
            return Response.error("Failed to delete student account: " + e.getMessage());
        }
    }

    @Override
    public Response sendCredentials(Long studentId) {
        return sendCredentials(studentId, null);
    }

    @Override
    public Response sendCredentials(Long studentId, DepartmentScope scope) {
        try {
            Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }

            User user = student.getUser();
            if (user == null || user.isDeleted()) {
                return Response.error("User not found");
            }

            // Generate new password
            String plainPassword = PasswordGenerator.generatePassword();
            String hashedPassword = passwordEncoder.encode(plainPassword);
            user.setPassword(hashedPassword);
            userRepository.save(user);

            // Send email
            boolean emailSent = emailUtil.sendStudentCredentialsEmail(
                    user.getEmail(),
                    user.getUsername(),
                    plainPassword);

            if (!emailSent) {
                return Response.error("Failed to send email");
            }

            return Response.success("Credentials sent successfully", null);

        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Error sending credentials: {}", e.getMessage(), e);
            return Response.error("Failed to send credentials: " + e.getMessage());
        }
    }

    @Override
    public Response bulkSendCredentials(BulkSendCredentialsRequest request) {
        return bulkSendCredentials(request, null);
    }

    @Override
    public Response bulkSendCredentials(BulkSendCredentialsRequest request, DepartmentScope scope) {
        try {
            if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
                return Response.error("Danh sách sinh viên không được để trống");
            }

            boolean managerScoped = scope != null && scope.manager() && !scope.admin();

            List<String> successList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();

            for (Long studentId : request.getStudentIds()) {
                try {
                    Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                            .orElse(null);

                    if (student == null) {
                        errorList.add("Student ID " + studentId + ": Not found");
                        continue;
                    }

                    if (managerScoped && !studentRepository.existsActiveByIdAndDepartmentIds(
                            studentId, scope.departmentIds())) {
                        errorList.add("Student ID " + studentId + ": Access denied");
                        continue;
                    }

                    User user = student.getUser();
                    if (user == null || user.isDeleted()) {
                        errorList.add("Student ID " + studentId + ": User not found");
                        continue;
                    }

                    // Generate new password
                    String plainPassword = PasswordGenerator.generatePassword();
                    String hashedPassword = passwordEncoder.encode(plainPassword);
                    user.setPassword(hashedPassword);
                    userRepository.save(user);

                    // Send email
                    boolean emailSent = emailUtil.sendStudentCredentialsEmail(
                            user.getEmail(),
                            user.getUsername(),
                            plainPassword);

                    if (emailSent) {
                        successList.add("Student ID " + studentId + " (" + user.getEmail() + ")");
                    } else {
                        errorList.add("Student ID " + studentId + ": Failed to send email");
                    }

                } catch (Exception e) {
                    logger.error("Error sending credentials for student {}: {}", studentId, e.getMessage(), e);
                    errorList.add("Student ID " + studentId + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successList", successList);
            result.put("errorList", errorList);
            result.put("successCount", successList.size());
            result.put("errorCount", errorList.size());

            return Response.success(
                    String.format("Sent credentials to %d students. %d errors.", successList.size(), errorList.size()),
                    result);

        } catch (Exception e) {
            logger.error("Error in bulkSendCredentials: {}", e.getMessage(), e);
            return Response.error("Failed to send credentials: " + e.getMessage());
        }
    }

    @Override
    public Response validateStudentAccount(String studentCode, String email) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();

            if (studentCode != null && !studentCode.trim().isEmpty()) {
                String code = studentCode.trim();
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, code, null);
                UserUniquenessHelper.reclaimDeletedStudentCode(studentRepository, userRepository, code);
                boolean usernameExists = userRepository.existsByUsernameAndIsDeletedFalse(code);
                boolean studentCodeExists = studentRepository.findByStudentCodeAndIsDeletedFalse(code).isPresent()
                        || studentRepository.findByUserUsernameAndIsDeletedFalse(code).isPresent();
                result.put("studentCodeAvailable", !usernameExists && !studentCodeExists);
                result.put("studentCode", code);
            }

            if (email != null && !email.trim().isEmpty()) {
                String mail = email.trim();
                UserUniquenessHelper.reclaimDeletedIdentifiers(userRepository, null, mail);
                boolean emailExists = userRepository.existsByEmailAndIsDeletedFalse(mail);
                result.put("emailAvailable", !emailExists);
                result.put("email", mail);
            }

            return Response.success("Validation completed", result);
        } catch (Exception e) {
            logger.error("Error validating student account: {}", e.getMessage(), e);
            return Response.error("Failed to validate: " + e.getMessage());
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void applyDepartmentToStudent(Student student, Long departmentId, DepartmentScope scope) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        if (scope != null && scope.manager() && !scope.admin()
                && !scope.departmentIds().contains(departmentId)) {
            throw new IllegalArgumentException("Department is outside your scope");
        }

        student.setDepartment(department);
    }

    private void fillDepartmentFields(StudentAccountResponse response, Student student) {
        if (student.getDepartment() != null) {
            response.setDepartmentId(student.getDepartment().getId());
            response.setDepartmentName(student.getDepartment().getName());
        }
    }
}
