package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/11 15:44
 */
@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {

    @Autowired
    CourseBaseService courseBaseService;

    @ApiOperation("课程分页查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required=false) QueryCourseParamsDto queryCourseParamsDto) {

        return courseBaseService.queryCourseBaseList(pageParams, queryCourseParamsDto);

    }
    //@Validated开启实体类注解异常功能
    @PostMapping("/course")
     public CourseBaseInfoDto createCourseBase(@RequestBody @Validated({ValidationGroups.Inster.class}) AddCourseDto dto){
        Long companyId = 123214425L;
        return courseBaseService.createCourseBase(companyId,dto);
    }
    @ApiOperation("课程根据id查询接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        return courseBaseService.getCourseBaseById(courseId);
    }
    @ApiOperation("课程根据id修改接口")
    @PutMapping("/course")
    public CourseBaseInfoDto updateCourseBaseById(@RequestBody @Validated({ValidationGroups.Update.class})EditCourseDto editCourseDto){
        Long companyId = 1232141425L;
        return courseBaseService.updateCourseBaseById(companyId,editCourseDto);
    }
}
