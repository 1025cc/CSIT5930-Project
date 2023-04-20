package com.csit5930.searchengine;

import com.csit5930.searchengine.indexer.Indexer;
import com.csit5930.searchengine.utils.PageRank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {


    // Method to start the crawling process
    public void startCrawling(String seedUrl, int maxPages) {
        // Implement the logic to start the web crawler with the given seed URL and maximum number of pages to crawl
    }

    // Method to index the crawled pages
    public void indexPages() {
        // Implement the logic to index the crawled pages
    }

    // Method to compute PageRank for the indexed pages
    public void calculatePageRank() {


    }

    //todo crawl
    public static void main(String[] args) {
        Indexer indexer = new Indexer();
        List<Integer> pageIds = indexer.getAllPageIds();
        Map<Integer, Set<Integer>> childToParentLinks = new HashMap<>();
        Map<Integer, Set<Integer>> parentToChildLinks =new HashMap<>();
        for(int pageId:pageIds){
            Set<Integer> childPages = indexer.getChildIdsByPageId(pageId);
            Set<Integer> parentPages = indexer.getParentIdsByPageId(pageId);
            childToParentLinks.put(pageId,childPages);
            parentToChildLinks.put(pageId,parentPages);
        }
        PageRank pageRank = new PageRank(childToParentLinks,parentToChildLinks);
        pageRank.computePageRanks();
        pageRank.normalizePageRanks();
        for(int pageId:pageIds){
            double pageRankValue = pageRank.getPageRank(pageId);
            indexer.putPageRankValue(pageId,pageRankValue);
        }
    }
}
