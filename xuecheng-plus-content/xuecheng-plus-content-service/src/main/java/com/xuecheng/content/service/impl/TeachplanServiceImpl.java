package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Override
    public void deleteTeachplanById(Long teachplanId) {
        //1.删除第一级大章节,大章节下有小章节时不允许删除
        //2.删除第一级大章节,大章节下没有小章节时可以正常删除
        //3.删除小章节,同时将关联的信息进行删除

        //1.查询要删除的对象
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan.getGrade() == 1) {
            //查询要删除的对象下面是否还有小章节
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid, teachplanId);
            int count = teachplanMapper.selectCount(queryWrapper);
            if (count > 0) {
                XueChengPlusException.cast("要删除的大章节下面还有小章节,不能删除!");
            }
            else {
                //2.删除大章节,同时将其其他信息进行删除
                teachplanMapper.deleteById(teachplanId);
            }
        }
        if (teachplan.getGrade() == 2) {
            //3.删除小章节,同时将其其他信息进行删除
            teachplanMapper.deleteById(teachplanId);
        }
    }

    @Override
    public void moveUp(Long teachplanId) {
        //通过id查询
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer currentOrderby = teachplan.getOrderby();
        //判断该对象是否已经是排序的第一位,如果是则无需进行操作
        if (currentOrderby == 1) {
            XueChengPlusException.cast("已经是排序第一位!");
        }
        //根据该对象的排序值,查询出前一位对象
//        Teachplan preTeachplan = teachplanMapper.selectOne(new QueryWrapper<Teachplan>().eq("orderby", currentOrderby - 1L));
        Teachplan preTeachplan = teachplanMapper.selectOne(new QueryWrapper<Teachplan>()
                //一定要注意SQL语句和报错信息!
                .eq("parentId",teachplan.getParentid())
                .eq("course_Id",teachplan.getCourseId())
                .eq("orderby", currentOrderby - 1));
        if (preTeachplan == null) {
            XueChengPlusException.cast(CommonError.OBJECT_NULL);
        }
        //将该对象的排序值减1,并更新到数据库中
        teachplan.setOrderby(currentOrderby - 1);
        teachplanMapper.updateById(teachplan);
        //将前一位的对象的排序值加1,并更新到数据库中
        preTeachplan.setOrderby(currentOrderby);
        teachplanMapper.updateById(preTeachplan);
    }


    @Override
    public void moveDown(Long teachplanId) {
        //根据id查询出对象,并得到orderby的值
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer currentOrderby = teachplan.getOrderby();
        //根据currentOrderby查询出下一位对象
        Teachplan nextTeachplan = teachplanMapper.selectOne(new QueryWrapper<Teachplan>()
                .eq("parentId",teachplan.getParentid())
                .eq("course_Id",teachplan.getCourseId())
                .eq("orderby", currentOrderby + 1));
        if (nextTeachplan == null) {
            XueChengPlusException.cast("已经是排序的最后一位!");
        }
        //将这两两对象的排序倒换
        teachplan.setOrderby(currentOrderby + 1);
        teachplanMapper.updateById(teachplan);
        nextTeachplan.setOrderby(currentOrderby);
        teachplanMapper.updateById(nextTeachplan);
    }


}
