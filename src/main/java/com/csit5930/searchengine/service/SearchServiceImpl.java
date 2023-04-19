package com.csit5930.searchengine.service;

import com.csit5930.searchengine.indexer.Indexer;
import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.model.SearchResult;
import com.csit5930.searchengine.utils.Tokenizer;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {
    private final double TITLE_BOOST_FACTOR = 3.0;
    @Autowired
    private Indexer indexer;


    @Override
    public List<SearchResult> search(String query) throws RocksDBException {
        List<String> queryTokens = Tokenizer.tokenize(query);
        //page id->(token,tf)
        Map<Integer, Map<String, Double>> documentVectors = buildDocumentVectors(queryTokens);

        //Calculate score for ranking
        //page id->score
        HashMap<Integer, Double> documentScores = computeScoresOnContent(queryTokens, documentVectors);
        //Boost rankings for title matches
        for (String token : queryTokens) {
            List<Integer> titleMatches = indexer.getTitleMatchedPages(token);
            if (titleMatches != null) {
                for (int pageId : titleMatches) {
                    double existingScore = documentScores.getOrDefault(pageId, 0.0);
                    documentScores.put(pageId, existingScore * TITLE_BOOST_FACTOR);
                }
            }
        }
        List<SearchResult> results = processResults(documentScores);
        return results;
    }

    private List<SearchResult> processResults(HashMap<Integer, Double> documentScores) {
        return null;
    }

    private HashMap<Integer, Double> computeScoresOnContent(List<String> queryTokens, Map<Integer, Map<String, Double>> documentVectors) {
        return null;
    }

    /**
     * 构造文档向量
     * 1.for each token，get posting list，for each posting list, get tf, then calculate weight
     * should notice the special handle for phrase
     *
     * @param queryTokens
     * @return
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
    private void calculateTfIdf(String token, HashMap<Integer,Integer> pageIdToTf,Map<Integer, Map<String, Double>> documentVectors) {
        HashMap<Integer,Double> pageIdToTfIdf = new HashMap<>();
        for(Map.Entry<Integer,Integer> entry:pageIdToTf.entrySet()){
            int pageId = entry.getKey();
            double tfMax = indexer.getTfMax(pageId);
            double tf = entry.getValue();
            double df = pageIdToTf.size();
            double N = indexer.getTotalDocumentNum();
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

        // Retrieve posting lists for each word in the phrase
        List<Set<Integer>> pageIdsList = new ArrayList<>();
        for (String word : words) {
            List<Posting> postingList = indexer.getPostingListByWord(word);
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
    public HashMap<Integer,Integer> computeTermFreqWord(String word) {
        HashMap<Integer,Integer> termFreq = new HashMap<>();
        //Get a list of postings according to the word
        List<Posting> postingList = indexer.getPostingListByWord(word);
        //get tf_ij for word i in page j
        for (Posting posting : postingList) {
            int tf = posting.getTermFreq();
            int pageId = posting.getPageID();
            termFreq.put(pageId, tf);
        }
        return termFreq;
    }

}
