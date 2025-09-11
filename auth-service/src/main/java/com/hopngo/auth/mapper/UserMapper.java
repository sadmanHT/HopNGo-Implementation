package com.hopngo.auth.mapper;

import com.hopngo.auth.dto.UserDto;
import com.hopngo.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for converting between User entity and UserDto
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    /**
     * Convert User entity to UserDto
     * @param user the User entity
     * @return UserDto
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "verifiedProvider", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserDto toDto(User user);
    
    /**
     * Convert UserDto to User entity
     * @param userDto the UserDto
     * @return User entity
     */
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserDto userDto);
}