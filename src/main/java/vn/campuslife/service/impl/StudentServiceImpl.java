package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.StudentService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public Long getStudentIdByUsername(String username) {
        Optional<vn.campuslife.entity.Student> studentOpt = studentRepository
                .findByUserUsernameAndIsDeletedFalse(username);
        return studentOpt.map(vn.campuslife.entity.Student::getId).orElse(null);
    }

    @Override
    public Long getStudentIdByUserId(Long userId) {
        Optional<vn.campuslife.entity.Student> studentOpt = studentRepository
                .findByUserIdAndIsDeletedFalse(userId);
        return studentOpt.map(vn.campuslife.entity.Student::getId).orElse(null);
    }
}
