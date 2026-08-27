package org.example.aishop.util;

import org.example.aishop.dto.UserDTO;

public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();
    public  static void saveUser(UserDTO userDTO){
        tl.set(userDTO);
    }
    public static UserDTO getUser(){
        return tl.get();
    }

    public static Long getUserId() {
        UserDTO user = tl.get();
        return user != null ? user.getId() : null;
    }

    public static Long getShopId() {
        UserDTO user = tl.get();
        return user != null ? user.getShopId() : null;
    }

    public static void removeUser(){tl.remove();}
}
