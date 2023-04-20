package com.csit5930.searchengine.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PageRank {
    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    private Map<Integer, Set<Integer>> childToParentLinks;
    private Map<Integer, Set<Integer>> parentToChildLinks;
    private Map<Integer, Double> pageRanks;

    public PageRank(Map<Integer, Set<Integer>> childToParentLinks, Map<Integer, Set<Integer>> parentToChildLinks) {
        this.childToParentLinks = childToParentLinks;
        this.parentToChildLinks = parentToChildLinks;
        pageRanks = new HashMap<>();
    }


    public void computePageRanks() {
        // Initialize page ranks
        for (Integer page : parentToChildLinks.keySet()) {
            pageRanks.put(page, 1.0);
        }

        // Iterate until convergence
        boolean converged = false;
        for (int iteration = 0; !converged && iteration < MAX_ITERATIONS; iteration++) {
            Map<Integer, Double> newPageRanks = new HashMap<>();
            //used for checking convergence
            double maxChange = 0;

            for (int pageId : parentToChildLinks.keySet()) {
                double rank = (1 - DAMPING_FACTOR) / parentToChildLinks.size();
                for (int incomingPage : childToParentLinks.getOrDefault(pageId, new HashSet<>())) {
                    rank += DAMPING_FACTOR * pageRanks.get(incomingPage) / parentToChildLinks.get(incomingPage).size();
                }

                maxChange = Math.max(maxChange, Math.abs(rank - pageRanks.get(pageId)));
                newPageRanks.put(pageId, rank);
            }

            pageRanks = newPageRanks;
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

        double minPageRank = Double.MAX_VALUE;
        double maxPageRank = Double.MIN_VALUE;

        // Find the minimum and maximum PageRank values
        for (double pageRank : pageRanks.values()) {
            minPageRank = Math.min(minPageRank, pageRank);
            maxPageRank = Math.max(maxPageRank, pageRank);
        }

        // Normalize the PageRank values
        for (Integer pageId : pageRanks.keySet()) {
            double normalizedPageRank = (pageRanks.get(pageId) - minPageRank) / (maxPageRank - minPageRank);
            pageRanks.put(pageId, normalizedPageRank);
        }
    }


}

