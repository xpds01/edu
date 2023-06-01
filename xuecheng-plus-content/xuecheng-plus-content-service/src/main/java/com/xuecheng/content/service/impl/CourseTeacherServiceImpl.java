package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseTeacherServiceImpl extends ServiceImpl<CourseTeacherMapper, CourseTeacher> implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;
    @Override
    public List<CourseTeacher> queryCourseTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        //根据课程id查询
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return  courseTeachers;
    }

    @Override
    public CourseTeacher addCourseTeacher(CourseTeacher courseTeacher) {
        //如果通过id查询的对象不存在则新增
        Long id = courseTeacher.getId();
        CourseTeacher courseTeacherNew = courseTeacherMapper.selectById(id);
        if(courseTeacherNew==null){
            courseTeacherMapper.insert(courseTeacher);
            return courseTeacher;
        }
        //存在则修改
        else {
            BeanUtils.copyProperties(courseTeacher,courseTeacherNew);
            //替换id
            courseTeacherNew.setId(id);
        }
        courseTeacherMapper.updateById(courseTeacherNew);
        return courseTeacherNew;

    }

    @Override
    public CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher) {
        courseTeacherMapper.updateById(courseTeacher);
        return courseTeacher;
    }

    @Override
    public void deleteCourseTeacher(Long courseId, Long id) {
        //根据courseId和id查询
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,id);
        courseTeacherMapper.delete(queryWrapper);

    }
}
