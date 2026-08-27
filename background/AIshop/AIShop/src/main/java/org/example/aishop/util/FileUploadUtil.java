package org.example.aishop.util;

import org.example.aishop.common.enums.ImageTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUploadUtil {

    @Value("${file.local.upload-path}")
    private String uploadPath;

    @Value("${file.local.access-prefix}")
    private String accessPath;

    // 允许的图片类型
    private static final String[] ALLOWED_TYPE = {
            "image/jpg", "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    /**
     * 校验图片文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        // 校验类型
        String contentType = file.getContentType();
        boolean isAllowed = false;
        for (String type : ALLOWED_TYPE) {
            if (type.equalsIgnoreCase(contentType)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            throw new IllegalArgumentException("仅支持上传 jpg/jpeg/png/gif/webp 格式图片");
        }

        // 大小校验 10MB
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
    }

    /**
     * 上传图片
     */
    public String uploadImage(MultipartFile file, String imageType) throws IOException {
        validateFile(file);

        // 获取分类
        ImageTypeEnum typeEnum = ImageTypeEnum.getByDirName(imageType);
        String typeDir = typeEnum.getDirName();

        // 目录
        //地址+ditname/分类 创建目录
        String fullUploadPath = uploadPath + File.separator + typeDir + File.separator;
        File dir = new File(fullUploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 文件名 = uuid+originalname后缀
        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf("."));
        String fileName = UUID.randomUUID() + suffix;

        // 保存  地址+文件名
        File destFile = new File(fullUploadPath + fileName);
        file.transferTo(destFile);
        //返回网页地址 映射
        return accessPath + typeDir + "/" + fileName;
    }
}