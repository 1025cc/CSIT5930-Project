package com.csit5930.searchengine.model;

import java.util.LinkedHashMap;
import java.util.Set;

public class SearchResult {
    private String title;
    private String url;
    private String lastModifiedDate;
    private int size;
    private LinkedHashMap<String, Integer> top5Keywords;
    private Set<String> parentLinks;
    private Set<String> childLinks;

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

    public Set<String> getParentLinks() {
        return parentLinks;
    }

    public void setParentLinks(Set<String> parentLinks) {
        this.parentLinks = parentLinks;
    }

    public Set<String> getChildLinks() {
        return childLinks;
    }

    public void setChildLinks(Set<String> childLinks) {
        this.childLinks = childLinks;
    }

}

