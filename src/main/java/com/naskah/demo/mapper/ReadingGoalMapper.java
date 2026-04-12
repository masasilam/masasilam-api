package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ReadingGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingGoalMapper {

    void insert(ReadingGoal goal);

    void update(ReadingGoal goal);

    void delete(@Param("id") Long id, @Param("userId") Long userId);

    ReadingGoal findById(@Param("id") Long id);

    List<ReadingGoal> findByUser(@Param("userId") Long userId);

    List<ReadingGoal> findActiveByUser(@Param("userId") Long userId);

    List<ReadingGoal> findCompletedByUser(@Param("userId") Long userId);

    int countByUser(@Param("userId") Long userId);

    int countActiveByUser(@Param("userId") Long userId);

    int countCompletedByUser(@Param("userId") Long userId);

    int countThisMonthByUser(@Param("userId") Long userId);

    void updateCurrentValue(@Param("id") Long id, @Param("currentValue") int currentValue);

    void markCompleted(@Param("id") Long id);
}