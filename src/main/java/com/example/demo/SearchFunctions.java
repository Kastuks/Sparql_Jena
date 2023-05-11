package com.example.demo;

import javax.validation.constraints.Digits;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.jose.util.IntegerUtils;

public class SearchFunctions {
    public static String addXWordContext(String search) {
        String newSearch = "";
        String replacedWord = "";
        String words[] = search.split(" ");
        for (String word : words) {
            if (word.contains("{") && word.contains("}")) {
                String tempWord = word.replace("{", "").replace("}", "");
                try {
                    Integer.parseInt(tempWord);
                } catch (Exception e) {
                    System.out.println("Couldn't parse given X word context - " + e);
                    return search;
                }

                replacedWord = "(.*\\\\w+.*){0," + tempWord + "}" ;

            } else {
                replacedWord = word;
            }
            newSearch = newSearch.concat(replacedWord + " ");
            replacedWord = "";
        }
        return(StringUtils.chop(newSearch));
    }

    public static String searchToRegex(String search) {
        String newSearch = "";
        String replacedWord = "";
        String words[] = search.split(" ");
        for (String word : words) {
            if (word.contains("?")) {
                replacedWord = word.replace("?", "\\\\w");
                replacedWord = replacedWord.replace(replacedWord, "(\\\\b" + replacedWord + ")");

            } else if (word.contains("*")) {
                replacedWord = word.replace("*", "\\\\w+");
                replacedWord = replacedWord.replace(replacedWord, "(\\\\b" + replacedWord + ")");
            }

            newSearch = newSearch.concat(replacedWord + " ");
            replacedWord = "";
        }
        // StringUtils.chop(newSearch);
        return StringUtils.chop(newSearch);
    }

}
