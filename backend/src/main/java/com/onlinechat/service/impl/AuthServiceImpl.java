package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlinechat.common.ResultCode;
import com.onlinechat.config.JwtProperties;
import com.onlinechat.dto.LoginDTO;
import com.onlinechat.dto.UserRegisterDTO;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.security.JwtTokenProvider;
import com.onlinechat.service.AuthService;
import com.onlinechat.service.FriendGroupService;
import com.onlinechat.vo.LoginVO;
import com.onlinechat.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final FriendGroupService friendGroupService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(UserRegisterDTO dto) {
        if (existsByUsername(dto.getUsername())) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        if (StringUtils.hasText(dto.getEmail()) && existsByEmail(dto.getEmail())) {
            throw new BusinessException(ResultCode.CONFLICT, "邮箱已被使用");
        }
        if (StringUtils.hasText(dto.getPhone()) && existsByPhone(dto.getPhone())) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已被使用");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setNickname(dto.getNickname().trim());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        friendGroupService.getOrCreateDefaultGroup(user.getId());
        return buildLoginVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        friendGroupService.getOrCreateDefaultGroup(user.getId());
        return buildLoginVO(user);
    }

    @Override
    public UserVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toUserVO(user);
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username.trim())) > 0;
    }

    private boolean existsByEmail(String email) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email.trim())) > 0;
    }

    private boolean existsByPhone(String phone) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone.trim())) > 0;
    }

    private LoginVO buildLoginVO(User user) {
        return LoginVO.builder()
                .token(jwtTokenProvider.generateToken(user.getId(), user.getUsername()))
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMillis() / 1000)
                .user(toUserVO(user))
                .build();
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
