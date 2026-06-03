package com.onlinechat.service;

import com.onlinechat.common.RequestStatus;
import com.onlinechat.common.ResultCode;
import com.onlinechat.dto.FriendRequestCreateDTO;
import com.onlinechat.dto.FriendRequestHandleDTO;
import com.onlinechat.entity.FriendGroup;
import com.onlinechat.entity.FriendRequest;
import com.onlinechat.entity.Friendship;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.FriendRequestMapper;
import com.onlinechat.mapper.FriendshipMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.impl.FriendRequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceImplTest {

    @Mock
    private FriendRequestMapper friendRequestMapper;
    @Mock
    private FriendshipMapper friendshipMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FriendService friendService;
    @Mock
    private FriendGroupService friendGroupService;
    @InjectMocks
    private FriendRequestServiceImpl friendRequestService;

    @Test
    void createRequestRejectsSelf() {
        FriendRequestCreateDTO dto = new FriendRequestCreateDTO();
        dto.setReceiverId(1L);

        assertThatThrownBy(() -> friendRequestService.createRequest(1L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void createRequestInsertsPendingRequest() {
        FriendRequestCreateDTO dto = new FriendRequestCreateDTO();
        dto.setReceiverId(2L);
        dto.setMessage("hello");
        User receiver = new User();
        receiver.setId(2L);
        receiver.setStatus(1);

        when(userMapper.selectById(2L)).thenReturn(receiver);
        when(friendService.areFriends(1L, 2L)).thenReturn(false);
        when(friendRequestMapper.selectCount(any())).thenReturn(0L);

        friendRequestService.createRequest(1L, dto);

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestMapper).insert(captor.capture());
        FriendRequest request = captor.getValue();
        assertThat(request.getSenderId()).isEqualTo(1L);
        assertThat(request.getReceiverId()).isEqualTo(2L);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getMessage()).isEqualTo("hello");
    }

    @Test
    void acceptCreatesBidirectionalFriendship() {
        FriendRequest request = new FriendRequest();
        request.setId(10L);
        request.setSenderId(1L);
        request.setReceiverId(2L);
        request.setStatus(RequestStatus.PENDING);

        FriendGroup receiverGroup = group(20L);
        FriendGroup senderGroup = group(10L);
        FriendRequestHandleDTO dto = new FriendRequestHandleDTO();
        dto.setRemark("Alice");

        when(friendRequestMapper.selectOne(any())).thenReturn(request);
        when(friendService.areFriends(1L, 2L)).thenReturn(false);
        when(friendGroupService.getOrCreateDefaultGroup(2L)).thenReturn(receiverGroup);
        when(friendGroupService.getOrCreateDefaultGroup(1L)).thenReturn(senderGroup);

        friendRequestService.accept(2L, 10L, dto);

        ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipMapper, org.mockito.Mockito.times(2)).insert(friendshipCaptor.capture());
        assertThat(friendshipCaptor.getAllValues())
                .extracting(Friendship::getUserId, Friendship::getFriendId, Friendship::getGroupId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 1L, 20L),
                        org.assertj.core.groups.Tuple.tuple(1L, 2L, 10L)
                );
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        verify(friendRequestMapper).updateById(request);
    }

    private FriendGroup group(Long id) {
        FriendGroup group = new FriendGroup();
        group.setId(id);
        return group;
    }
}
