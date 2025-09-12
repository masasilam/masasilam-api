//package com.naskah.demo.service.impl;
//
//import com.naskah.demo.exception.custom.DataNotFoundException;
//import com.naskah.demo.mapper.UserMapper;
//import com.naskah.demo.model.entity.User;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class UserDetailsServiceImpl implements UserDetailsService {
//
//    private final UserMapper userMapper;
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userMapper.findUserByUsername(username);
//        if (user == null) {
//            throw new DataNotFoundException();
//        }
//        return org.springframework.security.core.userdetails.User.builder()
//                .username(user.getUsername())
//                .password(user.getPasswordHash())
//                .roles(userMapper.findUserRoles(user.getId()).stream()
//                        .map(role -> role.getName().replace("ROLE_", ""))
//                        .toArray(String[]::new))
//                .accountExpired(false)
//                .accountLocked(false)
//                .credentialsExpired(false)
//                .disabled(!user.getIsActive())
//                .build();
//    }
//}
