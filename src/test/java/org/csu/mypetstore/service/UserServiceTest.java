package org.csu.mypetstore.service;

import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findsUserByUsername() {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setDisplayName("林同学");
        alice.setRole("CUSTOMER");
        alice.setStatus("ENABLED");
        when(userMapper.selectOne(any())).thenReturn(alice);

        Optional<User> found = userService.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("林同学");
    }

    @Test
    void returnsEmptyWhenUserMissing() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThat(userService.findByUsername("nobody")).isEmpty();
    }
}
