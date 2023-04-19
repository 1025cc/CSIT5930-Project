package com.csit5930.searchengine.model;

import java.io.Serializable;

/**
 * Save some useful page information when crawling
 */
public class WebPage implements Serializable {
    private String title;
    private String url;
    private String lastModifiedDate;
    private String pageSize;
    private int tfMax;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public int getTfMax() {
        return tfMax;
    }

    public void setTfMax(int tfMax) {
        this.tfMax = tfMax;
    }
}
