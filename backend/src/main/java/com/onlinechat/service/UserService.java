package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.vo.UserSearchVO;

public interface UserService {

    PageResult<UserSearchVO> search(Long currentUserId, String keyword, long pageNo, long pageSize);
}
