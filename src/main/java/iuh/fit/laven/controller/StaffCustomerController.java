package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.LockUserRequest;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff (và Admin) quản lý khách hàng: xem, khóa, mở khóa (UC30). Quyền cấu hình ở SecurityConfig: /api/staff/**. */
@RestController
@RequestMapping("/api/staff/customers")
@RequiredArgsConstructor
public class StaffCustomerController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> search(@RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(userService.searchCustomers(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.getCustomer(id));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<UserResponse> lock(@PathVariable("id") String id,
                                             @RequestBody LockUserRequest request) {
        return ResponseEntity.ok(userService.lockCustomer(id, request));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<UserResponse> unlock(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.unlockCustomer(id));
    }
}