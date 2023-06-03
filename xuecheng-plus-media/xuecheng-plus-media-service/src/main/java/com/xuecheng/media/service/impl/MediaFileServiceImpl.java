package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
 @Slf4j
public class MediaFileServiceImpl implements MediaFileService {

  @Autowired
 MediaFilesMapper mediaFilesMapper;
  @Autowired
 MinioClient minioClient;
  @Value("${minio.bucket.files}")
  private String bucket_files;
  @Value("${minio.bucket.videofiles}")
  private String bucket_video;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 //根据扩展名获取到mineType
 public String getMimeType(String extension) {
  if (extension == null || extension.trim().length() == 0) {
      extension = "";
  }
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if(extensionMatch!=null){
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }

 //上传文件至minio
 public boolean uploadFileToMinio(String bucketName,String objectName,String localFilePath,String mineType){
     try {
         minioClient.uploadObject(UploadObjectArgs.builder()
                 .bucket(bucketName)
                 .object(objectName)
                 .filename(localFilePath)
                 .contentType(mineType)
                 .build());
         log.debug("文件上传到minio成功,bucket:{},objectName:{}",bucketName,objectName);
         return true;
     } catch (Exception e) {
         e.printStackTrace();
         log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucketName,objectName,e.getMessage());
     }
     return false;
 }


 //创建形式为yyyy-MM-dd的当前日期,并转为yyyy/MM/dd/的形式
public String getFolder(){
     String folder = new SimpleDateFormat("yyyy-MM-dd").format(new Date()).replace("-", "/") + "/";
     return folder;
}
//获取到文件的md5值

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
  //根据UploadFileParamsDto得到传过来的文件名
     String filename = uploadFileParamsDto.getFilename();
     //根据filename得到文件扩展名,如.mp4
     String extension = filename.substring(filename.lastIndexOf("."));
     //将文件扩展名设置为mineType
     String mineType = getMimeType(extension);
     String folder = getFolder();
     //把folder+文件的md5值+扩展名设置为objectName
     String fileMd5 = getFileMd5(new File(localFilePath));
     String objectName = folder + fileMd5 + extension;
     //上传文件指minio
     boolean uploadFileToMinio = uploadFileToMinio(bucket_files, objectName, localFilePath, mineType);
     if (!uploadFileToMinio) {
         XueChengPlusException.cast("文件上传是失败!");
     }
     //将文件信息存储到数据库
     MediaFiles mediaFiles = addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_files,objectName);
     if (mediaFiles == null) {
         XueChengPlusException.cast("文件信息保存失败!");
     }
     //返回结果
     UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
     BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
     return uploadFileResultDto;
 }

    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
        //从数据库查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            //保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件信息到数据库失败,{}",mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }
            log.debug("保存文件信息到数据库成功,{}",mediaFiles.toString());

        }
        return mediaFiles;

    }


}
