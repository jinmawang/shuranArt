package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Value("${upload.base-url:https://tianma.chat}")
    private String baseUrl;

    @Value("${upload.path:/app/uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 限制文件大小 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保目录存在
        Path dir = Paths.get(uploadPath);
        Files.createDirectories(dir);

        // 保存文件
        Path filePath = dir.resolve(filename);
        file.transferTo(filePath.toFile());

        // 返回完整可访问的 URL
        String url = baseUrl + "/images/uploads/" + filename;
        return Result.success(Map.of("url", url));
    }
}
