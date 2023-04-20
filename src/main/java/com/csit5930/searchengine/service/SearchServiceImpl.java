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
    private final int TITLE_BOOST_FACTOR = 5;
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
            //combine the pagerank values with cosine similarity
            double cosineSimilarity = calculateCosineSimilarity(queryTokens, documentVector);
            double pageRankValue = indexer.getPageRankValue(pageId);
            double score = 0.4 * pageRankValue + 0.6 * cosineSimilarity;
            HashMap.SimpleEntry<Integer,Double> scoreEntry = new HashMap.SimpleEntry<>(pageId,score);
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
            double N = indexer.getTotalPageNum();
            double tfIdf = (tf/tfMax) * log2(N/df);
            documentVectors.computeIfAbsent(pageId, k -> new HashMap<>()).put(token,tfIdf);
        }
    }
    public double log2(double x) {
        return  (Math.log(x) / Math.log(2.0));
    }

    private HashMap<Integer,Integer> computeTermFreqPhrase(String phrase) {
        // Tokenize the phrase
        String[] words = phrase.split(" ");

        //Page ids of all words in the phrase appear together
        HashSet<Integer> contentIntersection = new HashSet<>();
        HashSet<Integer> titleIntersection = new HashSet<>();
        // Retrieve posting lists for each word in the phrase
        for (int i = 0;i<words.length;i++) {
            words[i] = Tokenizer.tokenizeSingle(words[i]);
            List<Posting> contentPostingList = indexer.getContentPostingListByWord(words[i]);
            List<Posting> titlePostingList = indexer.getTitlePostingListByWord(words[i]);
            intersect(contentIntersection, contentPostingList);
            intersect(titleIntersection, titlePostingList);
        }

        // Check if the word positions form a valid phrase in each page content
        HashMap<Integer, Integer> result = new HashMap<>();
        for (int pageId : contentIntersection) {
            int validCount = countPhraseOccurrences(words, pageId,0);
            if (validCount > 0) {
                result.put(pageId, validCount);
            }
        }
        // Check if the word positions form a valid phrase in each page title
        for (int pageId : titleIntersection) {
            int validCount = countPhraseOccurrences(words, pageId,1);
            if (validCount > 0) {
                result.put(pageId, result.getOrDefault(pageId, 1) * TITLE_BOOST_FACTOR);
            }
        }
        return result;
    }

    private void intersect(HashSet<Integer> titleIntersection, List<Posting> titlePostingList) {
        if (titlePostingList != null) {
            //Get all page ids for calculating intersection
            Set<Integer> pageIds = new HashSet<>();
            for(Posting posting:titlePostingList){
                pageIds.add(posting.getPageID());
            }
            if (titleIntersection.isEmpty()) {
                titleIntersection.addAll(pageIds);
            } else {
                titleIntersection.retainAll(pageIds);
            }
        }
    }

    /**
     *
     * @param words
     * @param pageId
     * @param flag 0-in content 1-in title
     * @return
     */
    private int countPhraseOccurrences(String[] words, int pageId,int flag) {
        int validCount = 0;
        List<Integer> firstWordPosition = indexer.getWordPositions(words[0], pageId,flag);
        for (int i = 0; i < firstWordPosition.size(); i++) {
            int position = firstWordPosition.get(i);
            boolean isPhrase = true;
            // Check if the subsequent words in the phrase have positions incremented by 1
            for (int j = 1; j < words.length; j++) {
                List<Integer> curWordPosition = indexer.getWordPositions(words[j], pageId,flag);
                if (!curWordPosition.contains(position+1)) {
                    isPhrase = false;
                    break;
                }
                position++;
            }
            if (isPhrase) {
                validCount++;
            }
        }
        return validCount;
    }

    public HashMap<Integer,Integer> computeTermFreqWord(String word){
        HashMap<Integer,Integer> termFreq = new HashMap<>();
        //Get a list of postings according to the word
        List<Posting> contentPostingList = indexer.getContentPostingListByWord(word);
        List<Posting> titlePostingList = indexer.getTitlePostingListByWord(word);
        //get tf_ij for word i in page j
        for (Posting posting : contentPostingList) {
            int tf = posting.getTermFreq();
            int pageId = posting.getPageID();
            termFreq.put(pageId, tf);
        }
        //boosting title matching
        for (Posting posting : titlePostingList) {
            int pageId = posting.getPageID();
            int tf = termFreq.getOrDefault(pageId,1) * TITLE_BOOST_FACTOR;
            termFreq.put(pageId,tf);
        }
        return termFreq;
    }

}
