package org.example.aishop.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件访问控制器，用于提供上传图片的访问服务
 * 替代 WebConfig 中的静态资源处理器，避免与 UploadController 的 @RequestMapping("/upload") 路径冲突
 */
@Tag(name = "文件访问", description = "上传图片的静态资源访问")
@RestController
public class FileController {

    @Value("${file.local.upload-path}")
    private String uploadPath;

    /**
     * 处理所有 /upload/** 的图片请求
     * 支持多级子目录路径（如 /upload/产品图/华为Mate60Pro/华为Mate60Pro(2).jpg）
     */
    @Operation(summary = "图片文件访问", description = "读取 /upload/** 路径下的上传图片，支持中文路径和子目录")
    @GetMapping("/upload/**")
    public ResponseEntity<Resource> getFile(HttpServletRequest request) {
        // 从完整请求 URI 中提取相对路径（去掉 context-path 和 /upload/ 前缀）
        String requestURI = request.getRequestURI();
        String relativePath = requestURI.substring(
                requestURI.indexOf("/upload/") + "/upload/".length());

        if (relativePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // ★ URL 解码：浏览器传过来的中文会变成百分号编码（如 %E4%BA%A7%E5%93%81%E5%9B%BE）
        // 必须解码后才能匹配实际文件
        relativePath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);

        // 拼出服务器上的完整文件路径
        File file = new File(uploadPath, relativePath);

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        // 根据扩展名推断 Content-Type
        String contentType = guessContentType(relativePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String guessContentType(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }
}
