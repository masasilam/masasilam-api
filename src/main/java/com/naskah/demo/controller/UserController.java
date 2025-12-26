package com.naskah.demo.controller;

import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserResponse;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.service.impl.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<DataResponse<List<UserResponse>>> getAllUsers() {
        DataResponse<List<UserResponse>> response = userService.getAllUsers();

        return ResponseEntity.ok(response);
    }
}