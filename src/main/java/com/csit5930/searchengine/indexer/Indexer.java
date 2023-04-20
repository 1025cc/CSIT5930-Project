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

    /**
     * all useful indexes
     */
    public static final String WORD_TO_WORD_ID = "word_to_word_id";
    public static final String URL_TO_PAGE_ID = "url_to_page_id";
    public static final String WORD_ID_TO_POSTING = "word_id_to_posting";
    public static final String WORD_ID_TO_POSTING_TITLE = "word_id_to_posting_title";
    public static final String PAGE_ID_TO_PAGE_INFO = "page_id_to_page_info";
    public static final String PAGE_ID_TO_CHILD_PAGES = "page_id_to_child_pages";
    public static final String PAGE_ID_TO_PARENT_PAGES = "page_id_to_parent_pages";
    //forward index, storing the keywords and frequencies for each page
    public static final String PAGE_ID_TO_KEYWORDS = "page_id_to_keywords";
    public Indexer(){
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add(WORD_TO_WORD_ID);
        columnFamilies.add(URL_TO_PAGE_ID);
        columnFamilies.add(WORD_ID_TO_POSTING);
        columnFamilies.add(WORD_ID_TO_POSTING_TITLE);
        columnFamilies.add(PAGE_ID_TO_PAGE_INFO);
        columnFamilies.add(PAGE_ID_TO_CHILD_PAGES) ;
        columnFamilies.add(PAGE_ID_TO_PARENT_PAGES) ;
        columnFamilies.add(PAGE_ID_TO_KEYWORDS);
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
        List<String> pageTitleWords = Tokenizer.tokenize(webPage.getTitle());
        //get word positions in this page for all words
        HashMap<String, List<Integer>> wordPositionsInBody = calculateWordPositions(pageBodyWords);
        HashMap<String, List<Integer>> wordPositionsInTitle = calculateWordPositions(pageTitleWords);
        //get the max tf of this page
        int maxTf = getMaxTF(wordPositionsInBody);
        //construct a page info to persistently store
        //use some info from the crawler
        PageInfo pageInfo = webPage;
        pageInfo.setTfMax(maxTf);
        //generate pageId
        int pageId = getPageIdByUrl(pageInfo.getUrl());
        rocksDBUtil.put(PAGE_ID_TO_PAGE_INFO,pageId,pageInfo);
        //generate forward index
        HashMap<String,Integer> keywordFreqMap = getKeywordFreqMap(wordPositionsInBody);
        rocksDBUtil.put(PAGE_ID_TO_KEYWORDS,pageId,keywordFreqMap);
        //generate body inverted index
        generateInvertedIndex(pageBodyWords, wordPositionsInBody, pageId, WORD_ID_TO_POSTING);
        //generate title inverted index
        generateInvertedIndex(pageTitleWords, wordPositionsInTitle, pageId, WORD_ID_TO_POSTING_TITLE);
        //store N->number of all pages
        addTotalPageNum(1);
    }

    private void generateInvertedIndex(List<String> words, HashMap<String, List<Integer>> wordPositions, int pageId, String indexType) {
        for(String word: words){
            int wordId = getWordIdByWord(word);
            List<Integer> wordPosition = wordPositions.get(word);
            Posting posting= new Posting(pageId,wordPosition.size(),wordPosition);
            List<Posting> postingList= (List<Posting>) rocksDBUtil.get(indexType,wordId);
            if(postingList == null){
                postingList = new ArrayList<>();
            }
            postingList.add(posting);
            rocksDBUtil.put(indexType,wordId,postingList);
        }
    }

    /**
     * update an exist web page
     * @param webPage
     */
    public void updatePage(WebPage webPage){
        int pageId = getPageIdByUrl(webPage.getUrl());
        deletePageById(pageId);
        indexPage(webPage);
    }

    /**
     * delete a page
     * @param pageId
     */
    public void deletePageById(int pageId) {
        //use forward indexes to speed up deletion
        HashMap<String,Integer> keywordFreqMap = getKeywordsByPageId(pageId);
        for(String word:keywordFreqMap.keySet()){
            int wordId = getWordIdByWord(word);
            // Remove the old posting for this page
            List<Posting> postingList = (List<Posting>) rocksDBUtil.get(WORD_ID_TO_POSTING, wordId);
            postingList.removeIf(p -> p.getPageID() == pageId);
        }
        //delete title inverted index
        String title = getPageInfoById(pageId).getTitle();
        List<String> titleTokens = Tokenizer.tokenize(title);
        for(String word:titleTokens){
            int wordId =  getWordIdByWord(word);
            // Remove the old posting for this page
            List<Posting> postingList = (List<Posting>) rocksDBUtil.get(WORD_ID_TO_POSTING_TITLE, wordId);
            postingList.removeIf(p -> p.getPageID() == pageId);
        }
        rocksDBUtil.delete(PAGE_ID_TO_KEYWORDS,pageId);
        rocksDBUtil.delete(URL_TO_PAGE_ID,pageId);
        rocksDBUtil.delete(PAGE_ID_TO_PAGE_INFO,pageId);
        rocksDBUtil.delete(PAGE_ID_TO_CHILD_PAGES,pageId);
        rocksDBUtil.delete(PAGE_ID_TO_PARENT_PAGES,pageId);
        //update the total num of pages
        addTotalPageNum(-1);
    }

    /**
     * maintain the total number of all pages
     * @param num
     */
    public void addTotalPageNum(int num){
        //store N->number of all pages
        byte[] N = SerializationUtil.serialize("N");
        byte[] NValue = rocksDBUtil.get(N);
        if(NValue == null){
            rocksDBUtil.put(N,SerializationUtil.serialize(num));
        }else {
            int tmp = (int)SerializationUtil.deserialize(NValue);
            rocksDBUtil.put(N,SerializationUtil.serialize(tmp+num));
        }
    }


    private HashMap<String, Integer> getKeywordFreqMap(HashMap<String, List<Integer>> wordPositions) {
        HashMap<String,Integer> keywordFreqMap = new HashMap<>();
        for (String keyword : wordPositions.keySet()) {
            keywordFreqMap.put(keyword,wordPositions.get(keyword).size());
        }
        return keywordFreqMap;
    }

    private HashMap<String, Integer> getKeywordsByPageId(int pageId){
        HashMap<String, Integer> keywordFreqMap = (HashMap<String, Integer>) rocksDBUtil.get(PAGE_ID_TO_KEYWORDS,pageId);
        return keywordFreqMap;
    }

    /**
     * if existed, get
     * else, new
     * @param word
     * @return corresponding word id
     */
    private int getWordIdByWord(String word) {
        Integer wordId = (Integer) rocksDBUtil.get(WORD_TO_WORD_ID,word);
        if(wordId == null){
            int newWordId = wordIdCounter.getAndIncrement();
            rocksDBUtil.put(WORD_TO_WORD_ID,word,newWordId);
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
        byte[] wordId = rocksDBUtil.get(WORD_TO_WORD_ID,SerializationUtil.serialize(word));
        byte[] tmp = rocksDBUtil.get(WORD_ID_TO_POSTING, wordId);
        Object o =  SerializationUtil.deserialize(tmp);
        List<Posting> postings = o == null?new ArrayList<>():(List<Posting>) o;
        return postings;
    }
    public List<Posting> getTitlePostingListByWord(String word) {
        byte[] wordId = rocksDBUtil.get(WORD_TO_WORD_ID,SerializationUtil.serialize(word));
        byte[] tmp = rocksDBUtil.get(WORD_ID_TO_POSTING_TITLE, wordId);
        Object o =  SerializationUtil.deserialize(tmp);
        List<Posting> postings = o == null?new ArrayList<>():(List<Posting>) o;
        return postings;
    }

    /**
     *
     * @param word
     * @param pageId
     * @param flag 0-in content 1-in title
     * @return
     */
    public List<Integer> getWordPositions(String word,int pageId,int flag) {
        List<Posting> postings;
        if(flag == 0){
            postings = getContentPostingListByWord(word);
        }else{
            postings = getContentPostingListByWord(word);
        }
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

    public int getTotalPageNum() {
        //get total num of pages from the default column family
        byte[] tmp = rocksDBUtil.get(SerializationUtil.serialize("N"));
        int total = (int)SerializationUtil.deserialize(tmp);
        return total;
    }

    /**
     *
     * @param pageId
     * @return PageInfo
     * if not exist, return null
     */
    public PageInfo getPageInfoById(int pageId) {
        byte[] tmp = rocksDBUtil.get(PAGE_ID_TO_PAGE_INFO, SerializationUtil.serialize(pageId));
        PageInfo pageInfo = (PageInfo) SerializationUtil.deserialize(tmp);
        return pageInfo;
    }

    /**
     *
     * @param url
     * @return PageInfo
     * if not exist, return null
     */
    public PageInfo getPageInfoByUrl(String url) {
        int pageId = getPageIdByUrl(url);
        return getPageInfoById(pageId);
    }

    /**
     *
     * @param pageId
     * @return up to five most frequent stemmed keywords
     */
    public LinkedHashMap<String, Integer> getTop5KeywordByPageId(int pageId) {
        int topK = 5;
        byte[] tmp = rocksDBUtil.get(PAGE_ID_TO_KEYWORDS,SerializationUtil.serialize(pageId));
        if(tmp == null){
            return new LinkedHashMap<>();
        }
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
            Map.Entry<String, Integer> entry = minHeap.poll();
            topFrequentKeywords.put(entry.getKey(), entry.getValue());
        }

        return topFrequentKeywords;
    }

    /**
     * !!remember to close the db!!
     */
    public void close() {
        if (rocksDBUtil != null) {
            rocksDBUtil.close();
        }
    }

    /**
     * some link relation functions
     */

    /**
     * if existed, get
     * else, new
     * @param url
     * @return corresponding page id
     */
    private int getPageIdByUrl(String url){
        Integer pageId = (Integer) rocksDBUtil.get(URL_TO_PAGE_ID,url);
        if(pageId == null){
            int newPageId = pageIdCounter.getAndIncrement();
            rocksDBUtil.put(URL_TO_PAGE_ID,url,newPageId);
            return newPageId;
        }else {
            return pageId;
        }
    }

    /**
     *
     * @param pageIds
     * @return corresponding urls
     */
    private Set<String> getUrlsByIds(Set<Integer> pageIds) {
        Set<String> urls = new HashSet<>();
        for(int pageId:pageIds){
            PageInfo pageInfo = getPageInfoById(pageId);
            urls.add(pageInfo.getUrl());
        }
        return urls;
    }

    /**
     *
     * @param urls
     * @return corresponding pageIds
     */
    private Set<Integer> getPageIdsByUrls(Set<String> urls) {
        Set<Integer> pageIds = new HashSet<>();
        for(String url:urls){
            int pageId = getPageIdByUrl(url);
            pageIds.add(pageId);
        }
        return pageIds;
    }

    /**
     * @param url
     * @param parentLinks
     */
    public void addParentLinks(String url,Set<String> parentLinks){
        int pageId = getPageIdByUrl(url);
        Set<Integer> currentParentPageIds= getParentIdsByPageId(pageId);
        //convert all added parent urls to ids
        Set<Integer> addedParentPageIds = getPageIdsByUrls(parentLinks);
        currentParentPageIds.addAll(addedParentPageIds);
        rocksDBUtil.put(PAGE_ID_TO_PARENT_PAGES,pageId,currentParentPageIds);
    }

    /**
     * given a page id , return all urls of its parent pages
     * @param pageId
     * @return
     */
    public Set<String> getParentLinksByPageId(int pageId) {
        //get all parent page ids
        Set<Integer> pageIds = getParentIdsByPageId(pageId);
        //convert page ids to urls
        Set<String> pageUrls = getUrlsByIds(pageIds);
        return pageUrls;
    }
    /**
     * given a page id , return all ids of its parent page
     * @param pageId
     * @return set guarantees no duplication ids
     */
    private Set<Integer> getParentIdsByPageId(int pageId) {
        byte[] tmp = rocksDBUtil.get(PAGE_ID_TO_PARENT_PAGES,SerializationUtil.serialize(pageId));
        if(tmp == null){
            return new HashSet<>();
        }
        Set<Integer> pageIds = (Set<Integer>) SerializationUtil.deserialize(tmp);
        return pageIds;
    }


    /**
     * @param url
     * @param childLinks
     */
    public void addChildLinks(String url,Set<String> childLinks){
        int pageId = getPageIdByUrl(url);
        Set<Integer> currentChildPageIds= getParentIdsByPageId(pageId);
        //convert all added child urls to ids
        Set<Integer> addedChidlPageIds = getPageIdsByUrls(childLinks);
        currentChildPageIds.addAll(addedChidlPageIds);
        rocksDBUtil.put(PAGE_ID_TO_CHILD_PAGES,pageId,currentChildPageIds);
    }

    /**
     * given a page id , return all urls of its child pages
     * @param pageId
     * @return
     */
    public Set<String> getChildLinksByPageId(int pageId) {
        //get all child page ids
        Set<Integer> pageIds = getChildIdsByPageId(pageId);
        //convert page ids to urls
        Set<String> pageUrls = getUrlsByIds(pageIds);
        return pageUrls;
    }

    /**
     * given a page id , return all ids of its parent page
     * @param pageId
     * @return set guarantees no duplication ids
     */
    private Set<Integer> getChildIdsByPageId(int pageId) {
        byte[] tmp = rocksDBUtil.get(PAGE_ID_TO_CHILD_PAGES,SerializationUtil.serialize(pageId));
        if(tmp == null){
            return new HashSet<>();
        }
        Set<Integer> pageIds = (Set<Integer>) SerializationUtil.deserialize(tmp);
        return pageIds;
    }

    public void displayAllIndex(){
        rocksDBUtil.displayAllIndexes();
    }
}
