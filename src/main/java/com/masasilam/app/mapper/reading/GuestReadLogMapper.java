package com.masasilam.app.mapper.reading;

import com.masasilam.app.model.entity.GuestReadLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuestReadLogMapper {
    boolean existsByGuestAndBook(@Param("guestId") String guestId, @Param("bookId") Long bookId);
    boolean existsByGuestAndZine(@Param("guestId") String guestId, @Param("zineId") Long zineId);
    void insert(GuestReadLog log);
}