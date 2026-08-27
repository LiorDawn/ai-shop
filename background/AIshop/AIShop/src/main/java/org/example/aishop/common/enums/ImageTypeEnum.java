package org.example.aishop.common.enums;

import lombok.Getter;

@Getter
public enum ImageTypeEnum {

    AVATAR("avatar", "用户头像"),
    GOODS("goods", "商品图片"),
    BLOG("blog", "博客图片"),
    COMMENT("comment", "评价图片"),
    AI_CHAT("ai_chat", "AI对话图片"),
    OTHER("other", "其他图片");

    private final String dirName;
    private final String desc;

    ImageTypeEnum(String dirName, String desc) {
        this.dirName = dirName;
        this.desc = desc;
    }

    public static ImageTypeEnum getByDirName(String dirName) {
        for (ImageTypeEnum type : values()) {
            if (type.getDirName().equals(dirName)) {
                return type;
            }
        }
        return OTHER;
    }
}