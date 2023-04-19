package com.csit5930.searchengine.indexer;

import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.model.PageInfo;
import com.csit5930.searchengine.model.WebPage;
import com.csit5930.searchengine.utils.RocksDBUtil;
import com.csit5930.searchengine.utils.SerializationUtil;
import com.csit5930.searchengine.utils.Tokenizer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Indexer {
    private RocksDBUtil rocksDBUtil;
    private final String dbPath = "./db";

    /**
     * Temporary implementation: always count from 0
     * Can consider persistently store the word and page last IDs
     */
    private static final AtomicInteger wordIdCounter = new AtomicInteger(0);
    private static final AtomicInteger pageIdCounter = new AtomicInteger(0);

    public Indexer(){
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_to_word_id");
        columnFamilies.add("url_to_page_id");
        columnFamilies.add("word_id_to_posting");
        columnFamilies.add("page_id_to_page_info");
        columnFamilies.add("page_id_to_child_pages") ;
        columnFamilies.add("page_id_to_parent_pages") ;
        //forward index, storing the keywords and frequencies for each page
        columnFamilies.add("page_id_to_keywords");
        rocksDBUtil = new RocksDBUtil(dbPath, columnFamilies);
    }

    /**
     * index a new web page
     * @param webPage
     */
    public void indexPage(WebPage webPage){
        String pageBody = webPage.getBody();
        //stemming and stopword removing
        List<String> pageBodyWords = Tokenizer.tokenize(pageBody);
        //get word positions in this page for all words
        HashMap<String, List<Integer>> wordPositionsInBody = calculateWordPositions(pageBodyWords);
        //get the max tf of this page
        int maxTf = getMaxTF(wordPositionsInBody);
        //construct a page info to persistently store
        //use some info from the crawler
        PageInfo pageInfo = webPage;
        pageInfo.setTfMax(maxTf);
        //generate pageId
        int pageId = pageIdCounter.getAndIncrement();
        rocksDBUtil.put("url_to_page_id",pageInfo.getUrl(),pageId);
        rocksDBUtil.put("page_id_to_page_info",pageId,pageInfo);
        //generate forward index
        HashMap<String,Integer> keywordFreqMap = getKeywordFreqMap(wordPositionsInBody);
        rocksDBUtil.put("page_id_to_keywords",pageId,keywordFreqMap);
        //generate inverted index
        for(String word: pageBodyWords){
            int wordId = getWordIdByWord(word);
            List<Integer> wordPosition = wordPositionsInBody.get(word);
            Posting posting= new Posting(pageId,wordPosition.size(),wordPosition);
            List<Posting> postingList= (List<Posting>) rocksDBUtil.get("word_id_to_posting",wordId);
            if(postingList == null){
                postingList = new ArrayList<>();
            }
            postingList.add(posting);
            rocksDBUtil.put("word_id_to_posting",wordId,postingList);
        }
    }

    private HashMap<String, Integer> getKeywordFreqMap(HashMap<String, List<Integer>> wordPositions) {
        HashMap<String,Integer> keywordFreqMap = new HashMap<>();
        for (String keyword : wordPositions.keySet()) {
            keywordFreqMap.put(keyword,wordPositions.get(keyword).size());
        }
        return keywordFreqMap;
    }

    private int getWordIdByWord(String word) {
        Integer wordId = (Integer) rocksDBUtil.get("word_to_word_id",word);
        if(wordId == null){
            int newWordId = wordIdCounter.getAndIncrement();
            rocksDBUtil.put("word_to_word_id",word,wordId);
            return newWordId;
        }else{
            return wordId;
        }

    }

    private HashMap<String, List<Integer>> calculateWordPositions(List<String> words) {
        HashMap<String, List<Integer>> wordPositions = new HashMap<String, List<Integer>>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            List<Integer> posList = wordPositions.getOrDefault(word, null);
            if (posList == null) {
                posList = new ArrayList<>();
                posList.add(i);
                wordPositions.put(word, posList);
            } else {
                posList.add(i);
            }
        }
        return wordPositions;
    }
    private int getMaxTF(HashMap<String, List<Integer>> wordPositions) {
        int max = 0;
        for (List<Integer> wordPosition : wordPositions.values()) {
            max = Math.max(max, wordPosition.size());
        }
        return max;
    }
    public List<Posting> getContentPostingListByWord(String word) {
        byte[] wordId = rocksDBUtil.get("word_to_word_id",SerializationUtil.serialize(word));
        byte[] tmp = rocksDBUtil.get("word_id_to_posting", wordId);
        List<Posting> postings = (List<Posting>) SerializationUtil.deserialize(tmp);
        return postings;
    }
    public List<Integer> getWordPositions(String word,int pageId) {
        List<Posting> postings = getContentPostingListByWord(word);
        List<Integer> wordPositions = new ArrayList<>();
        for(Posting posting:postings){
            if(posting.getPageID() == pageId){
                return posting.getWordPosition();
            }
        }
        return wordPositions;
    }

    public int getTfMax(int pageId) {
        PageInfo pageInfo = getPageInfoById(pageId);
        return pageInfo.getTfMax();
    }

    public int getTotalDocumentNum() {
        //get total num of documents from the default column family
        byte[] tmp = rocksDBUtil.get(SerializationUtil.serialize("N"));
        int total = (int)SerializationUtil.deserialize(tmp);
        return total;
    }


    public PageInfo getPageInfoById(int pageId) {
        byte[] tmp = rocksDBUtil.get("page_id_to_page_info", SerializationUtil.serialize(pageId));
        PageInfo pageInfo = (PageInfo) SerializationUtil.deserialize(tmp);
        return pageInfo;
    }

    public List<String> getChildLinksByPageId(int pageId) {
        byte[] tmp = rocksDBUtil.get("page_id_to_child_pages",SerializationUtil.serialize(pageId));
        List<Integer> pageIds = (List<Integer>) SerializationUtil.deserialize(tmp);
        List<String> pageUrls = getUrlsByIds(pageIds);
        return pageUrls;
    }

    private List<String> getUrlsByIds(List<Integer> pageIds) {
        List<String> urls = new ArrayList<>();
        for(int pageId:pageIds){
            PageInfo pageInfo = getPageInfoById(pageId);
            urls.add(pageInfo.getUrl());
        }
        return urls;
    }

    public List<String> getParentLinksByPageId(int pageId) {
        byte[] tmp = rocksDBUtil.get("page_id_to_parent_pages",SerializationUtil.serialize(pageId));
        List<Integer> pageIds = (List<Integer>) SerializationUtil.deserialize(tmp);
        List<String> pageUrls = getUrlsByIds(pageIds);
        return pageUrls;
    }

    public LinkedHashMap<String, Integer> getTop5KeywordByPageId(int pageId) {
        int topK = 5;
        byte[] tmp = rocksDBUtil.get("page_id_to_keywords",SerializationUtil.serialize(pageId));
        HashMap<String,Integer> keywordFreq = (HashMap<String,Integer> ) SerializationUtil.deserialize(tmp);
        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                (entry1, entry2) -> Integer.compare(entry1.getValue(), entry2.getValue())
        );

        for (Map.Entry<String, Integer> entry : keywordFreq.entrySet()) {
            minHeap.offer(entry);

            if (minHeap.size() > topK) {
                minHeap.poll();
            }
        }

        LinkedHashMap<String, Integer> topFrequentKeywords = new LinkedHashMap<>();
        while (!minHeap.isEmpty()) {
            topFrequentKeywords.put(minHeap.poll().getKey(),minHeap.poll().getValue());
        }

        return topFrequentKeywords;
    }
    public void close() {
        if (rocksDBUtil != null) {
            rocksDBUtil.close();
        }
    }
}
