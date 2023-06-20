package com.xuecheng;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: lxp
 * @description: TODO
 * @date: 2023/6/3 15:02
 * @version: 1.0
 */

public class MediaTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000/")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    //上传文件
    @Test
    public  void upload() {
        //根据扩展名取出mimeType   Maven: com.j256.simplemagic:simplemagic:1.17 (simplemagic-1.17.jar)
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")
//                    .object("test001.mp4")
                    .object("001/test001.mp4")//添加子目录
                    .filename("D:\\develop\\upload\\1mp4.temp")
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }

    @Test
    public void delete_Test() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("apache-tomcat-7.0.57.tar.gz").build();
        minioClient.removeObject(removeObjectArgs);
    }
    //查询文件 从minio中下载
    @Test
    public void test_getFile() throws Exception {

        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("test/01/1.mp4").build();
        //查询远程服务获取到一个流对象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        //指定输出流
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\develop\\upload\\1a.mp4"));
        IOUtils.copy(inputStream, outputStream);

        //校验文件的完整性对文件的内容进行md5
        FileInputStream fileInputStream1 = new FileInputStream(new File("D:\\develop\\upload\\1.mp4"));
        String source_md5 = DigestUtils.md5Hex(fileInputStream1);
        FileInputStream fileInputStream = new FileInputStream(new File("D:\\develop\\upload\\1a.mp4"));
        String local_md5 = DigestUtils.md5Hex(fileInputStream);
        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }
    }

    //测试文件分块的方法
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("E:\\.develop\\upload\\1.mp4");
        String chunkPath = "E:\\.develop\\upload\\chunk\\";
        File chunkFile = new File(chunkPath);
        if (!chunkFile.exists()) {
            chunkFile.mkdirs();
        }
        //分块大小
        long chunkSize = 1024 * 1024*5;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        //缓冲区大小
        byte[] buffer = new byte[1024];
        //使用RandomAccessFile访问文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile,"r");
        //分块
        for (long i = 0; i < chunkNum; i++) {
            //创建分块文件
            File chunk = new File(chunkPath + i );
            if (chunk.exists()) {
                chunk.delete();
            }
            boolean newFile = chunk.createNewFile();
            if (newFile){
                //写入分快
                RandomAccessFile raf_write = new RandomAccessFile(chunk,"rw");
               int len = -1;
               while ((len = raf_read.read(buffer))!=-1){
                   raf_write.write(buffer,0,len);
                   if(chunk.length()>=chunkSize){
                       break;
                   }
               }
               raf_write.close();
                System.out.println("完成分块：" + i);
            }
        }
        raf_read.close();
    }

    //测试文件合并
    @Test
    public void testMerge() throws IOException {
        //块文件目录
        File chunkFolder = new File("E:\\.develop\\upload\\chunk\\");
        //源文件
        File source = new File("E:\\.develop\\upload\\1.mp4");
        //合并文件
        File mergeFile = new File("E:\\.develop\\upload\\1a.mp4");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        //创建新的合并文件
        boolean newFile = mergeFile.createNewFile();
        //用RandomAccessFile写文件
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile,"rw");
        //指针指向文件顶端
        raf_write.seek(0);
        //缓冲区
        byte[] buffer = new byte[1024];
        //分块列表
        File[] chunkFiles = chunkFolder.listFiles();
        //转为集合
        List<File> fileList = Arrays.asList(chunkFiles);
        //根据名字从小到大排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        //合并文件
        for (File file : fileList) {
            //访问分快
            RandomAccessFile raf_read = new RandomAccessFile(file,"rw");
            int len = -1;
            while ((len = raf_read.read(buffer))!=-1){
                raf_write.write(buffer,0,len);
            }
            raf_read.close();
    }
        raf_write.close();
        FileInputStream fileInputStream = new FileInputStream(mergeFile);
        FileInputStream fileInputStream1 = new FileInputStream(source);
        String source_md5 = DigestUtils.md5Hex(fileInputStream1);
        String local_md5 = DigestUtils.md5Hex(fileInputStream);
        if (source_md5.equals(local_md5)) {
            System.out.println("合并文件成功");
        }else {
            System.out.println("合并文件失败");
        }
    }
    @Test
    public void uploadChunkToMinio() throws IOException {
        String chunkFolderPath = "E:\\.develop\\upload\\chunk\\";
        File chunkFolder = new File(chunkFolderPath);
        File[] chunkFiles = chunkFolder.listFiles();
        if (chunkFiles != null) {
            for (int i = 0 ;i<chunkFiles.length;i++) {
                try {
                    UploadObjectArgs testbucket = UploadObjectArgs.builder()
                            .bucket("testbucket")
    //                    .object("test001.mp4")
                            .object("chunk\\")//添加子目录
                            .filename(chunkFolderPath)
                            .build();
                    minioClient.uploadObject(testbucket);
                    System.out.println("上传成功"+i);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("上传失败");
                }
            }
        }
    }
    //合并minio文件
    @Test
    public void mergeMinioFile() throws Exception {
        //创建一个长度为6的流,将流里每个元素映射成composeSourse
        List<ComposeSource> sources = Stream.iterate(0,i -> ++i).limit(6)
                .map(i -> ComposeSource.builder().bucket("testbucket")
                        .object("chunk/".concat(Integer.toString(i)))//"to".concat("get").concat("her") returns "together"
                        .build())
                .collect(Collectors.toList());
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder().
                bucket("testbucket").object("merge01.mp4").sources(sources).build();
        minioClient.composeObject(composeObjectArgs);

    }
    //清除分块文件
    @Test
    public void removeObject() throws Exception {
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket("testbucket").build();
        Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);
        for (Result<Item> result : results) {
            Item item = result.get();
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object(item.objectName()).build();
            minioClient.removeObject(removeObjectArgs);
        }
        System.out.println("操作完毕");
    }
    }
