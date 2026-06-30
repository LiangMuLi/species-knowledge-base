package com.species.controller;

import com.species.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传 Controller
 *
 * 仅管理员可用，上传图片到本地 uploads 目录
 * 返回可访问的 URL 路径
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 上传图片
     * POST /api/upload/image
     *
     * 请求: multipart/form-data, file=图片文件
     * 响应: {"code":200, "data": {"url": "/uploads/xxx.jpg"}}
     */
    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }

        // 检查文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error(400, "仅支持 JPG/PNG/GIF/WebP 格式");
        }

        try {
            // 用绝对路径，避免被解析到 Tomcat 临时目录
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名，防止重名覆盖
            String ext = filename.substring(filename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回可访问的 URL
            String url = "/uploads/" + newFilename;
            return Result.success(Map.of("url", url));

        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
