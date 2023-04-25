package com.csit5930.searchengine.utils;


import com.csit5930.searchengine.indexer.Indexer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PageRank {
    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    private double pageRankSum = 0;

    private Map<Integer, Set<Integer>> childToParentLinks;
    private Map<Integer, Set<Integer>> parentToChildLinks;
    private Map<Integer, Double> pageRanks;

    public PageRank(Map<Integer, Set<Integer>> childToParentLinks, Map<Integer, Set<Integer>> parentToChildLinks) {
        this.childToParentLinks = childToParentLinks;
        this.parentToChildLinks = parentToChildLinks;
        pageRanks = new HashMap<>();
    }


    public void computePageRanks() {
        double size = parentToChildLinks.keySet().size();
        // Initialize page ranks
        for (Integer page : parentToChildLinks.keySet()) {
            pageRanks.put(page, 1.0/size);
        }

        // Iterate until convergence
        boolean converged = false;
        for (int iteration = 0; !converged && iteration < MAX_ITERATIONS; iteration++) {
            Map<Integer, Double> newPageRanks = new HashMap<>();
            //used for checking convergence
            double maxChange = 0;

            for (int pageId : parentToChildLinks.keySet()) {
                double rank = 1.0 - DAMPING_FACTOR;
                for (int incomingPage : childToParentLinks.getOrDefault(pageId, new HashSet<>())) {
                    rank += DAMPING_FACTOR * pageRanks.get(incomingPage) / parentToChildLinks.get(incomingPage).size();
                }

                maxChange = Math.max(maxChange, Math.abs(rank - pageRanks.get(pageId)));
                newPageRanks.put(pageId, rank);
                pageRankSum += rank;
            }

            pageRanks = newPageRanks;
            normalizePageRanks();
            pageRankSum = 0;
            converged = maxChange < CONVERGENCE_THRESHOLD;
        }
    }
    public double getPageRank(int pageId) {
        return pageRanks.getOrDefault(pageId, 0.0);
    }

    public void normalizePageRanks() {
        if (pageRanks.isEmpty()) {
            return;
        }

        // Normalize the PageRank values
        for (Integer pageId : pageRanks.keySet()) {
            double normalizedPageRank = pageRanks.get(pageId)/ pageRankSum;
            pageRanks.put(pageId, normalizedPageRank);
        }
    }

    /**
     * test function
     * @param args
     */
    public static void main(String[] args) {
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
    }

}

