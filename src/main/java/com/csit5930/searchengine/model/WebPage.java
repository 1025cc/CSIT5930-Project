package com.csit5930.searchengine.model;

public class WebPage extends PageInfo{

    private String body;
    public WebPage(String body, String title, String url, String lastModifiedDate, int pageSize){
        super(title,url,lastModifiedDate,pageSize);
        this.body = body;
    }
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
