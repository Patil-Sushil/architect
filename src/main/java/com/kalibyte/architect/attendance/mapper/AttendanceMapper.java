package com.kalibyte.architect.attendance.mapper;

import com.kalibyte.architect.attendance.entity.Attendance;
import com.kalibyte.architect.attendance.dto.AttendanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    AttendanceResponse toResponse(Attendance attendance);
}
