package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

public interface MediaProcessService {
    //根据分片参数获取待处理任务 如果任务id % 任务总数 = 任务序号,则可以执行  Service层不要用@Param!!!
    List<MediaProcess> getMediaProcessList(int shardTotal,int shardIndex,int count);
    //开启一个任务
    boolean startTask(long id);
    //保存任务结果
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);
}
