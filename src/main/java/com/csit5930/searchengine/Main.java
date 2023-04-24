package com.csit5930.searchengine;

import com.csit5930.searchengine.crawler.Crawler;
import com.csit5930.searchengine.indexer.Indexer;
import com.csit5930.searchengine.utils.PageRank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {


    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        //crawling and indexing
        logger.info("fetch started");
        Crawler.fetch();
        logger.info("fetch finished");
        //calculate page rank value
        logger.info("page rank start");
        Indexer indexer = new Indexer();
        List<Integer> pageIds = indexer.getAllPageIds();
        Map<Integer, Set<Integer>> childToParentLinks = new HashMap<>();
        Map<Integer, Set<Integer>> parentToChildLinks =new HashMap<>();
        for(int pageId:pageIds){
            Set<Integer> childPages = indexer.getChildIdsByPageId(pageId);
            Set<Integer> parentPages = indexer.getParentIdsByPageId(pageId);
            childToParentLinks.put(pageId,parentPages);
            parentToChildLinks.put(pageId,childPages);
        }
        PageRank pageRank = new PageRank(childToParentLinks,parentToChildLinks);
        pageRank.computePageRanks();
        for(int pageId:pageIds){
            double pageRankValue = pageRank.getPageRank(pageId);
            indexer.putPageRankValue(pageId,pageRankValue);
        }
        indexer.close();
        logger.info("page rank end");
    }
}
