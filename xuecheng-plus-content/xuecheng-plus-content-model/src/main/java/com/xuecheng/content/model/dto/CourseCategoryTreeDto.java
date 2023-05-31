package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

/**
 * @author: lxp
 * @description: 课程分类Dto
 * @date: 2023/5/26 20:37
 * @version: 1.0
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory implements java.io.Serializable{
    List<CourseCategoryTreeDto>  childrenTreeNodes;
}
