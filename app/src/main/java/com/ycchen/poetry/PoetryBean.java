package com.ycchen.poetry;

import java.io.Serializable;

/**
 * @author ycchen
 * @Description: 诗词 Bean
 * @since 2019/11/27 11:19
 */
public class PoetryBean implements Serializable {
    /**
     * content : 星垂平野阔，月涌大江流。
     * origin : 旅夜书怀
     * author : 杜甫
     * category : 古诗文-山水-长江
     */

    private String content;
    private String origin;
    private String author;
    private String category;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
