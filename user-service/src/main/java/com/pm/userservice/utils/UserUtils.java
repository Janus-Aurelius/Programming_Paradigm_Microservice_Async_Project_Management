package com.pm.userservice.utils;

import org.springframework.beans.BeanUtils;

import com.pm.commoncontracts.dto.UserDto;

public class UserUtils {
    public static UserDto entityToDto(com.pm.userservice.model.User user) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        return userDto;
    }

    public static com.pm.userservice.model.User dtoToEntity(UserDto userDto) {
        com.pm.userservice.model.User userEntity = new com.pm.userservice.model.User();
        BeanUtils.copyProperties(userDto, userEntity);
        userEntity.setFirstName(userDto.getFirstName());
        userEntity.setLastName(userDto.getLastName());
        return userEntity;
    }
}
