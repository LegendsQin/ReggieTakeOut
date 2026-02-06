package org.example.reggie.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {

        if(file.isEmpty()){
            log.info("文件为空");
            return R.error("文件为空");
        }

        String fileName = file.getOriginalFilename();
        if(fileName==null || fileName.isEmpty()){
            log.info("文件名不存在");
            return R.error("文件名不可用");
        }

        String suffix = fileName.substring(fileName.lastIndexOf("."));
        log.info("文件后缀为：{}",suffix);


        String newFileName = UUID.randomUUID().toString();
        newFileName += suffix;

        // 创建文件保存目录,防止文件目录不存在
        File dir = new File(basePath);
        if(!dir.exists()){
            boolean mkdirs = dir.mkdirs();
            if(mkdirs){
                log.info("创建目录成功");
            }
            else {
                log.info("创建目录失败");
                return R.error("文件上传失败, 创建目录失败");
            }
        }

        //将文件转存到目标地址
        try {
            file.transferTo(new File(basePath + newFileName));
            log.info("文件上传成功, 文件名: {}", newFileName);
            return R.success(newFileName);
        }
        catch (IOException e) {
            e.printStackTrace();
            log.info("文件上传失败, 文件名: {}", newFileName);
            return R.error("文件上传失败");
        }
    }

    @GetMapping("/download")
    public R<String> download(HttpServletResponse response, String name) {

        if(name==null || name.isEmpty()){
            log.info("文件名为空");
            return R.error("文件名为空");
        }

        try {
            // 创建输入流
            InputStream inputStream = new FileInputStream(new File(basePath+name));
            // 创建输出流
            ServletOutputStream outputStream = response.getOutputStream();


            //response.setContentType("image/jpeg");

            int len = 0;
            byte[] buffer = new byte[1024];

            // 读取到末尾（EOF，文件结束）：返回 -1
            while(len!=-1){
                len = inputStream.read(buffer);
                outputStream.write(buffer,0,len);
                outputStream.flush();
            }
            inputStream.close();
            outputStream.close();
            return R.success(name);
            // 输入流读取，输出流写入
        } catch (IOException e) {
            log.info("文件下载失败, 文件名: {}", name);
            return R.error("文件下载失败");
        }
    }
}
