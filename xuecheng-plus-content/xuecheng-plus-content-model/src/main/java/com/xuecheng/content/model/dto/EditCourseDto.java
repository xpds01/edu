package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author: lxp
 * @description: TODO
 * @date: 2023/5/30 20:38
 * @version: 1.0
 */
@Data
@ApiModel(value="EditCourseDto", description="修改课程基本信息")
public class EditCourseDto extends AddCourseDto{
    //修改课程需要知道id
    private Long id ;

}
