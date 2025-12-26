package com.naskah.demo.service.impl;

import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public DataResponse<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponse> users = userMapper.findAllUsers();

        log.info("Successfully fetched {} users", users.size());
        return new DataResponse<>("success", "Genres retrieved successfully", HttpStatus.OK.value(), users);
    }
}