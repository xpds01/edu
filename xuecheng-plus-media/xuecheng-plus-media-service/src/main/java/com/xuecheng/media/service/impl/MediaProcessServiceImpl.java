package com.xuecheng.media.service.impl;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaProcessService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: lxp
 * @description: 任务处理
 * @date: 2023/6/13 15:50
 * @version: 1.0
 */
@Service
public class MediaProcessServiceImpl implements MediaProcessService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0?false:true;
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //需要更新的任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess==null){
            return;
        }

        //如果任务执行失败
        if (status.equals("3")){
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            return;

            //将上面的更新方式更改为有效的更新方式,用mediaProcessMapper.update()
//            UpdateWrapper<MediaProcess> updateWrapper = new UpdateWrapper<>();
//            updateWrapper.eq("id",taskId);
//            updateWrapper.set("status","3");
//            updateWrapper.set("fail_count",mediaProcess.getFailCount()+1);
//            updateWrapper.set("errormsg",errorMsg);
//            mediaProcessMapper.update(mediaProcess,updateWrapper);

        }
        //如果任务执行成功
        //文件表记录
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        //更新media_file表中的url
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);

        //更新MediaProcess表中的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);

        //将MediaProcess表记录插入到MediaProcess表中

        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        //从MediaProcess删除当前任务

        mediaProcessMapper.deleteById(taskId);
    }
}
