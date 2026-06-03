package com.onlinechat.service;

import com.onlinechat.dto.LoginDTO;
import com.onlinechat.dto.UserRegisterDTO;
import com.onlinechat.vo.LoginVO;
import com.onlinechat.vo.UserVO;

public interface AuthService {

    LoginVO register(UserRegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO currentUser(Long userId);
}
