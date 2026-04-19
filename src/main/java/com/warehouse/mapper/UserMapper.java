package com.warehouse.mapper;

import com.warehouse.dto.response.UserResponse;
import com.warehouse.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
