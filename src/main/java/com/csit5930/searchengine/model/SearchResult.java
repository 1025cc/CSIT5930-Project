package com.csit5930.searchengine.model;

import java.util.LinkedHashMap;
import java.util.List;

public class SearchResult {
    private String title;
    private String url;
    private String lastModifiedDate;
    private int size;
    private LinkedHashMap<String, Integer> top5Keywords;
    private List<String> parentLinks;
    private List<String> childLinks;

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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public LinkedHashMap<String, Integer> getTop5Keywords() {
        return top5Keywords;
    }

    public void setTop5Keywords(LinkedHashMap<String, Integer> top5Keywords) {
        this.top5Keywords = top5Keywords;
    }

    public List<String> getParentLinks() {
        return parentLinks;
    }

    public void setParentLinks(List<String> parentLinks) {
        this.parentLinks = parentLinks;
    }

    public List<String> getChildLinks() {
        return childLinks;
    }

    public void setChildLinks(List<String> childLinks) {
        this.childLinks = childLinks;
    }

    // Add getters and setters here
}

