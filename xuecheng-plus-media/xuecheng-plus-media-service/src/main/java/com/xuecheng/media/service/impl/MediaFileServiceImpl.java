package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  @Autowired
  MediaFileService currentProxy;
  @Autowired
  MediaProcessMapper mediaProcessMapper;

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
         XueChengPlusException.cast("文件上传失败!");
     }
     //将文件信息存储到数据库  这里使用了代理对象调用事务方法,使得方法的事务生效
     MediaFiles mediaFiles =currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_files,objectName);
     if (mediaFiles == null) {
         XueChengPlusException.cast("文件信息保存失败!");
     }
     //返回结果
     UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
     BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
     return uploadFileResultDto;
 }
 //事务失效场景:当一个非事务方法调用一个事务方法,该事务方法会失效,事务生效条件:方法的调用者必须是代理对象且加了注解
    //为什么不在非事务方法加事务注解:像上传图片这种网络波动大的情况,比如会占用很长时间,用事务会占据太多资源
    //解决方法:将被调用的方法的对象注入进来,用这个代理对象调用事务方法
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
            //添加到待处理任务表
            addWaitingTask(mediaFiles);
            return mediaFiles;
        }
        return mediaFiles;

    }
    //添加媒资信息到待处理任务表
    private void addWaitingTask(MediaFiles mediaFiles) {
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mineType = getMimeType(extension);

        if (mineType.equals("video/x-msvideo")){ //avi的mineType
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            mediaFiles.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }

    }


    @Override
    public RestResponse<Boolean> checkFile(String fileMd5)  {
     //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);

        if (mediaFiles != null) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket)
                .object(filePath).build();
        //查询远程服务获取到一个流对象
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        }
        return RestResponse.success(false);

    }
    //查询分块是否存在
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        String chunkFolder = getChunkFolder(fileMd5);

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket_video)
                    .object(chunkFolder+chunkIndex)
                .build();
            //查询远程服务获取到一个流对象
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null) {
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String absolutePath, String fileMd5, int chunk) {
        String mimeType = getMimeType(null);//返回一个通用mimeType，字节流
        String chunkFolder = getChunkFolder(fileMd5);
        String objectName = chunkFolder+chunk;
        boolean b = uploadFileToMinio(bucket_video, objectName, absolutePath, mimeType);
        if (!b) {
            return RestResponse.validfail(false,"上传分块文件失败");
        }
        return RestResponse.success(true);
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件的所在目录
        String chunkFolder = getChunkFolder(fileMd5);
        //找到所有的分块文件
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder().bucket(bucket_video).object(chunkFolder+i).build())
                .collect(Collectors.toList());
        //源文件名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后的文件objectName
        String objectName = getMergeFolder(fileMd5,extension);
        //指定合并后的objectName等信息
        ComposeObjectArgs args = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)
                .sources(sources)
                .build();
        //合并文件
        try {
            minioClient.composeObject(args);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName{}",bucket_video,objectName);
            return RestResponse.validfail(false,"合并文件异常");
        }
        //校验合并后的文件是否一致
        File file = downloadFileFromMinIO(bucket_video, objectName);
        try {
            String md5Hex_download = DigestUtils.md5Hex(new FileInputStream(file));
            if (!md5Hex_download.equals(fileMd5)){
                log.error("校验文件md5值不一致,原始文件{},合并文件{}",fileMd5,md5Hex_download);
                return RestResponse.validfail(false,"文件校验失败");
            }
        } catch (IOException e) {
            return RestResponse.validfail(false,"文件校验失败");
        }
        //将文件信息入库
        uploadFileParamsDto.setFileSize(file.length());
        MediaFiles mediaFiles =currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_video,objectName);
        if (mediaFiles == null) {
            log.error("俱存文件信息到数据失败");
            return RestResponse.validfail(false,"保存文件信息失败");
        }
        //清理分块文件
        clearChunkFiles(chunkFolder,chunkTotal);
        return RestResponse.success(true);
    }

    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){

        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r->{
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清理分块文件失败,objectname:{}",deleteError.objectName(),e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清理分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
        }
    }

    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    //得到合并文件文件夹
    private String getMergeFolder(String fileMd5,String FileExt) {
     //根据传来的fileMd5分别以第一个字符和第二个字符作为一级路径和二级路径
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5+"/"+ fileMd5+FileExt;
    }

    private String getChunkFolder(String fileMd5) {
     //根据传来的fileMd5分别以第一个字符和第二个字符作为一级路径和二级路径
        String firstPath = fileMd5.substring(0, 1);
        String secondPath = fileMd5.substring(1, 2);
        return firstPath + "/" + secondPath + "/" + fileMd5+"/"+"chunk"+"/";
    }


}
