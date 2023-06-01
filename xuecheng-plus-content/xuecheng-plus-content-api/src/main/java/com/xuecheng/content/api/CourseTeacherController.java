package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: lxp
 * @description: 师资信息相关接口
 * @date: 2023/6/1 11:00
 * @version: 1.0
 */
@RestController
@RequestMapping("/courseTeacher")
public class CourseTeacherController {
    @Autowired
    CourseTeacherService courseTeacherService;
    @ApiOperation("课程课程师资信息查询")
    @GetMapping("/list/{courseId}")
    public List<CourseTeacher> queryCourseTeacherList(@PathVariable Long courseId) {
        return courseTeacherService.queryCourseTeacherList(courseId);
    }
    @ApiOperation("课程课程师资信息新增")
    @PostMapping()
    public CourseTeacher addCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return courseTeacherService.addCourseTeacher(courseTeacher);
    }
    @ApiOperation("课程课程师资信息修改")
    @PutMapping()
    public CourseTeacher updateCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return courseTeacherService.updateCourseTeacher(courseTeacher);
    }
    @ApiOperation("课程课程师资信息删除")
    @DeleteMapping("/course/{courseId}/{id}")
    public void deleteCourseTeacher(@PathVariable Long courseId, @PathVariable Long id) {
        courseTeacherService.deleteCourseTeacher(courseId, id);
    }





}
