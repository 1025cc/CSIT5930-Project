package com.csit5930.searchengine.model;

import java.io.Serializable;
import java.util.HashSet;

public class WebPage implements Serializable {
    public String title;
    public String url;
    public String lastModifiedDate;
    public HashSet<String> parentLinks = new HashSet<String>();
    public HashSet<String> childLinks;
    public String pageSize;
    public int maxTF;
}
