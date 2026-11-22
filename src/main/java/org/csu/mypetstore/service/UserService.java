package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service public class UserService {

private final UserMapper userMapper;

public UserService(UserMapper userMapper) {
    this.userMapper = userMapper;
}

public Optional<User> findByUsername(String username) {
    return Optional.ofNullable(userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
}

}
