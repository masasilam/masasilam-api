package com.naskah.demo.service.impl;

import com.naskah.demo.model.dto.response.DataResponse;
import com.naskah.demo.model.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    DataResponse <List<UserResponse>> getAllUsers();
}
