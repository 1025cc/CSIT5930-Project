package com.csit5930.searchengine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public Posting() {
        // empty constructor required for serialization
    }
    public Posting(int pageID, int termFreq,List<Integer> wordPosition)
    {
        this.pageID = pageID;
        this.termFreq = termFreq;
        this.wordPosition = wordPosition;
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

    public void setWordPosition(List<Integer> wordPosition){
        this.wordPosition = wordPosition;
    }
    @Override
    public String toString() {
        return "Posting{" +
                "pageID=" + pageID +
                ", termFreq=" + termFreq +
                ", wordPosition=" + wordPosition +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Posting posting = (Posting) o;
        return pageID == posting.pageID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageID);
    }
}
