package com.csit5930.searchengine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stored in inverted index file
 * Each term corresponds to a posting list
 */
public class Posting implements Serializable {
    /**
     * page id
     */
    private int pageID;
    /**
     * term i's term frequency in this page j
     */
    private int termFreq;
    /**
     * term i's positions in this page j
     * used for handling phrase search
     */
    private List<Integer> wordPosition;

    public Posting(int pageID, int termFreq)
    {
        this.pageID = pageID;
        this.termFreq = termFreq;
        this.wordPosition = new ArrayList<>();
    }


    boolean containsWordPos(int wordPos)
    {
        return wordPosition.contains(wordPos);
    }

    public int getPageID() {
        return pageID;
    }

    public void setPageID(int pageID) {
        this.pageID = pageID;
    }

    public int getTermFreq() {
        return termFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public List<Integer> getWordPosition(){
        return  wordPosition;
    }
}
