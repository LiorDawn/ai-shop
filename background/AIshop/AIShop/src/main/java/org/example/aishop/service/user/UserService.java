package org.example.aishop.service.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.user.User;

import java.util.List;

public interface UserService extends IService<User> {

    void addUser(User user);

    void deleteUser(Long id);

    void deleteBatchUsers(List<Long> ids);

    void updateUser(User user);

    void updateUserStatus(Long id, Integer status);

    UserDTO getUserById(Long id);

    List<UserDTO> listUsers();

    IPage<UserDTO> pageUsers(Integer current, Integer size, String username, String phone);
}
