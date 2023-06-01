package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-02-11
 */
public interface CourseTeacherService extends IService<CourseTeacher> {

     List<CourseTeacher> queryCourseTeacherList(Long courseId) ;

     CourseTeacher addCourseTeacher(CourseTeacher courseTeacher);

     CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher);

     void deleteCourseTeacher(Long courseId, Long id);
}
