package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
 @Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
 @RestController
public class MediaFilesController {


  @Autowired
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
  Long companyId = 1232141425L;
  return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);

 }

 @ApiOperation("上传文件")
 @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata) throws IOException {
     //MultipartFile是Spring MVC的接口，用于表示上传文件的数据。它有很多方法，可以用于获取文件名、文件大小、文件类型等信息，以及读取文件内容
     //consumes属性指定了请求的Content-Type为multipart/form-data。
     //@RequestPart("filedata")表示将HTTP请求中名为"filedata"的multipart/form-data部分绑定到方法的MultipartFile类型参数upload上
     Long  companyId = 1232141425L;
     UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
     uploadFileParamsDto.setFileSize(filedata.getSize());
     uploadFileParamsDto.setFileType("001001");
     uploadFileParamsDto.setFilename(filedata.getOriginalFilename());

     //设置临时文件
     File tempFile = File.createTempFile("minio", "temp");
     //将上传的文件拷贝到临时文件
     filedata.transferTo(tempFile);
     String absolutePath = tempFile.getAbsolutePath();
     return mediaFileService.uploadFile(companyId,uploadFileParamsDto,absolutePath);
 }

}
