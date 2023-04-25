package com.csit5930.searchengine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tokenizer
 */
public class Tokenizer {
    private static StopStem stopStem = new StopStem("stopwords.txt");

    /**
     * Including phrase identification, token stemming and stop words removing
     * @param queryString user input query string
     * @return a list of query tokens
     */
    public static List<String> tokenize(String queryString) {
        List<String> queryTokens = new ArrayList<>();

        // Convert queryString to lowercase
        queryString = queryString.toLowerCase();

        // Regular expression pattern to match words and phrases in double quotes
        Pattern pattern = Pattern.compile("\"[^\"]+\"|\\S+");
        Matcher matcher = pattern.matcher(queryString);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith("\"") && token.endsWith("\"")) {
                // Remove double quotes from phrases
                token = token.substring(1, token.length() - 1);
                queryTokens.add(token);
            }else{
                // Remove stop words
                if (!stopStem.isStopWord(token)) {
                    // Perform stemming
                    token = stopStem.stem(token);
                    queryTokens.add(token);
                }
            }
        }

        return queryTokens;
    }
    public static String tokenizeSingle(String queryString){
        // Convert queryString to lowercase
        queryString = queryString.toLowerCase();

        String token = null;
        // Remove stop words
        if (!stopStem.isStopWord(queryString)) {
            // Perform stemming
            token = stopStem.stem(queryString);
        }
        return token;
    }
}
