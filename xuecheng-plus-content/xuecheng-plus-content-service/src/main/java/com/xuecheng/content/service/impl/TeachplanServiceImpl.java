package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto teachplan) {
        //通过课程计划id判断是新增和修改
        Long id = teachplan.getId();
        if (id == null) {
            //新增
            Teachplan teachplanNew = new Teachplan();
            BeanUtils.copyProperties(teachplan, teachplanNew);
            //确定排序字段,找到它同级节点个数,排序字段就是个数+1,select count(1) from teachplan where course_id = ? and parentId = ?
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getCourseId, teachplan.getCourseId())
                            .eq(Teachplan::getParentid, teachplan.getParentid());
            int count = teachplanMapper.selectCount(queryWrapper);
            teachplanNew.setOrderby(count + 1);
            teachplanMapper.insert(teachplanNew);
        }
        else {
            //修改
            Teachplan teachplanNew = new Teachplan();
            BeanUtils.copyProperties(teachplan, teachplanNew);
            teachplanMapper.updateById(teachplanNew);
        }
    }
}
