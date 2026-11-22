package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.CreateStaffRequest;
import iuh.fit.laven.dto.request.LockUserRequest;
import iuh.fit.laven.dto.request.UpdateStaffRequest;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin quản lý tài khoản nhân viên (UC32). Quyền ADMIN cấu hình ở SecurityConfig: /api/admin/**. */
@RestController
@RequestMapping("/api/admin/staffs")
@RequiredArgsConstructor
public class AdminStaffController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.listStaffs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.getStaff(id));
    }

    /** Sửa thông tin nhân viên; không đổi email và mật khẩu. */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable("id") String id,
                                               @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(userService.updateStaff(id, request));
    }

    /** Khóa nhân viên: bắt buộc lý do trong body {"reason": "..."}. */
    @PostMapping("/{id}/lock")
    public ResponseEntity<UserResponse> lock(@PathVariable("id") String id,
                                             @RequestBody LockUserRequest request) {
        return ResponseEntity.ok(userService.lockStaff(id, request));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<UserResponse> unlock(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.unlockStaff(id));
    }
}