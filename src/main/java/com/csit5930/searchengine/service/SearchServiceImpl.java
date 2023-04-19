package com.csit5930.searchengine.service;

import com.csit5930.searchengine.indexer.Indexer;
import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.model.SearchResult;
import com.csit5930.searchengine.model.PageInfo;
import com.csit5930.searchengine.utils.Tokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    /**
     * favor the title matches
     */
    private final double TITLE_BOOST_FACTOR = 3.0;
    /**
     * the maximum num of the search results
     */
    private final int MAX_OUTPUT_NUM = 50;
    @Autowired
    private Indexer indexer;


    @Override
    public List<SearchResult> search(String query) {
        List<String> queryTokens = Tokenizer.tokenize(query);
        //page id->(token,tf)
        Map<Integer, Map<String, Double>> documentVectors = buildDocumentVectors(queryTokens);

        //Calculate score for ranking
        //page ids of top 50 pages
        List<Integer> top50Pages = getTopPages(queryTokens, documentVectors);

        List<SearchResult> results = processResults(top50Pages);
        return results;
    }

    private List<SearchResult> processResults(List<Integer> topPages) {
        List<SearchResult> results = new ArrayList<>();
        for(int pageId: topPages){
            SearchResult searchResult = new SearchResult();
            PageInfo pageInfo= indexer.getPageInfoById(pageId);
            searchResult.setTitle(pageInfo.getTitle());
            searchResult.setUrl(pageInfo.getUrl());
            searchResult.setSize(pageInfo.getPageSize());
            searchResult.setLastModifiedDate(pageInfo.getLastModifiedDate());
            searchResult.setTop5Keywords(indexer.getTop5KeywordByPageId(pageId));
            searchResult.setChildLinks(indexer.getChildLinksByPageId(pageId));
            searchResult.setParentLinks(indexer.getParentLinksByPageId(pageId));
            results.add(searchResult);
        }
        return results;
    }

    private List<Integer> getTopPages(List<String> queryTokens, Map<Integer, Map<String, Double>> documentVectors) {
        //using priority queue to get top 50 pages
        PriorityQueue<HashMap.SimpleEntry<Integer, Double>> ranking = new PriorityQueue<>(MAX_OUTPUT_NUM,
                (a, b) -> Double.compare(b.getValue(), a.getValue())
        );

        for (Map.Entry<Integer, Map<String, Double>> entry : documentVectors.entrySet()) {
            int pageId = entry.getKey();
            Map<String, Double> documentVector = entry.getValue();

            double cosineSimilarity = calculateCosineSimilarity(queryTokens, documentVector);
            HashMap.SimpleEntry<Integer,Double> scoreEntry = new HashMap.SimpleEntry<>(pageId,cosineSimilarity);
            if (ranking.size() < MAX_OUTPUT_NUM) {
                ranking.add(scoreEntry);
            } else if (cosineSimilarity > ranking.peek().getValue()) {
                // Remove the document with the lowest score
                ranking.poll();
                ranking.add(scoreEntry);
            }
        }
        //change to list
        List<Integer> topPages = ranking.stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return topPages;
    }
    private double calculateCosineSimilarity(List<String> queryKeywords, Map<String, Double> documentVector) {
        double queryMagnitude = Math.sqrt(queryKeywords.size());
        double documentMagnitude = Math.sqrt(documentVector.values().stream().mapToDouble(x -> x * x).sum());
        double dotProduct = documentVector.values().stream().mapToDouble(Double::doubleValue).sum();

        return dotProduct / (queryMagnitude * documentMagnitude);
    }
    /**
     *
     * For each token，get posting list，for each posting list, get tf, then calculate weight
     * should notice the special handle for phrase
     *
     * @param queryTokens words and phrases
     * @return documentVectors: page id-> (term, wight)...
     */
    private Map<Integer, Map<String, Double>> buildDocumentVectors(List<String> queryTokens) {
        Map<Integer, Map<String, Double>> documentVectors = new HashMap<>();

        for (String token : queryTokens) {
            //page id->tf
            HashMap<Integer,Integer> pageIdToTf;
            if (!token.contains(" ")) {
                //get word frequency
                pageIdToTf = computeTermFreqWord(token);
            } else {
                //get phrase frequency
                pageIdToTf = computeTermFreqPhrase(token);
            }
            calculateTfIdf(token, pageIdToTf,documentVectors);
        }
        return documentVectors;
    }



    /**
     * @param token
     * @param pageIdToTf
     * @param documentVectors
     */
    private void calculateTfIdf(String token, HashMap<Integer,Integer> pageIdToTf,Map<Integer, Map<String, Double>> documentVectors) {
        for(Map.Entry<Integer,Integer> entry:pageIdToTf.entrySet()){
            int pageId = entry.getKey();
            double tfMax = indexer.getTfMax(pageId);
            double tf = entry.getValue();
            double df = pageIdToTf.size();
            double N = indexer.getTotalDocumentNum();
            double tfIdf = (tf/tfMax) * log2(N/df);
            //boosting title matching
            if(isTokenInTitle(token,pageId)){
                tfIdf *= TITLE_BOOST_FACTOR;
            }
            documentVectors.computeIfAbsent(pageId, k -> new HashMap<>()).put(token,tfIdf);
        }
    }
    public boolean isTokenInTitle(String token, int pageId) {
        List<Posting> postings = indexer.getContentPostingListByWord(token);
        boolean res = false;
        for(Posting posting:postings){
            if(posting.getPageID() == pageId){
                PageInfo pageInfo = indexer.getPageInfoById(pageId);
                String title = pageInfo.getTitle();
                if(title.toLowerCase().contains(token.toLowerCase())){
                    res = true;
                    return res;
                }
            }
        }
        return res;
    }
    public double log2(double x) {
        return  (Math.log(x) / Math.log(2.0));
    }

    private HashMap<Integer,Integer> computeTermFreqPhrase(String phrase) {
        // Tokenize the phrase
        String[] words = phrase.split(" ");

        // Retrieve posting lists for each word in the phrase
        List<Set<Integer>> pageIdsList = new ArrayList<>();
        for (String word : words) {
            word = Tokenizer.tokenizeSingle(word);
            List<Posting> postingList = indexer.getContentPostingListByWord(word);
            if (postingList != null) {
                //Get all page ids for calculating intersection
                Set<Integer> pageIds = new HashSet<>();
                for(Posting posting:postingList){
                    pageIds.add(posting.getPageID());
                }
                pageIdsList.add(pageIds);
            }
        }

        // Intersect posting lists to find documents containing all words in the phrase
        HashSet<Integer> commonPageIds = getIntersection(pageIdsList);

        // Check if the word positions form a valid phrase in each document
        HashMap<Integer, Integer> result = new HashMap<>();
        for (Integer pageId : commonPageIds) {
            int validCount = 0;
            for (int i = 1; i < words.length; i++) {
                List<Integer> prevWordPositions = indexer.getWordPositions(words[i - 1], pageId);
                List<Integer> currWordPositions = indexer.getWordPositions(words[i], pageId);
                validCount = Math.min(validCount,countConsecutivePositions(prevWordPositions, currWordPositions));
            }
            if (validCount>0) {
                result.put(pageId,validCount);
            }
        }

        return result;
    }


    /**
     *
     * @param pageIdsList Page ids of all words in the phrase that have appeared
     * @return Page ids of all words in the phrase appear together
     */
    private HashSet<Integer> getIntersection(List<Set<Integer>> pageIdsList) {
        HashSet<Integer> intersection = new HashSet<>();
        for (Set<Integer> set : pageIdsList) {
            if (intersection.isEmpty()) {
                intersection.addAll(set);
            } else {
                intersection.retainAll(set);
            }
        }
        return intersection;
    }
    private int countConsecutivePositions(List<Integer> list1, List<Integer> list2) {
        int res = 0;
        int i = 0, j = 0;
        while (i < list1.size() && j < list2.size()) {
            if (list1.get(i) + 1 == list2.get(j)) {
                res++;
            } else if (list1.get(i) < list2.get(j)) {
                i++;
            } else {
                j++;
            }
        }
        return res;
    }
    public HashMap<Integer,Integer> computeTermFreqWord(String word){
        HashMap<Integer,Integer> termFreq = new HashMap<>();
        //Get a list of postings according to the word
        List<Posting> postingList = indexer.getContentPostingListByWord(word);
        //get tf_ij for word i in page j
        for (Posting posting : postingList) {
            int tf = posting.getTermFreq();
            int pageId = posting.getPageID();
            termFreq.put(pageId, tf);
        }
        return termFreq;
    }

}
