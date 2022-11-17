package com.ratedra;

import java.util.ArrayList;
import java.util.List;

/**
 * auto-completion based on trie.
 * first index a few search keywords to trie
 * and then search some incomplete text and the program should return
 * all the matching suggestions with that input search text
 */

class TrieNode{
    TrieNode[] children;

    boolean eow;
    public TrieNode() {
        eow = false;
        children = new TrieNode[26];
        for(int i=0; i<26; i++){
            children[i] = null;
        }
    }
}

class TrieUtil{
    TrieNode root;

    public TrieUtil() {
        root = new TrieNode();
    }

    public void insert(String word){
        TrieNode current = root;
        for(int i=0; i<word.length(); i++){
            char c = word.charAt(i);

            int index = c-'a';
            if(current.children[index] == null){
                current.children[index] = new TrieNode();
            }
            current = current.children[index];
        }
        current.eow = true;
    }

    public List<String> search(String searchText){
        List<String> suggestions = new ArrayList<>();
        TrieNode current = root;

        for(int i=0; i<searchText.length(); i++){
            char c = searchText.charAt(i);

            int index = c-'a';
            if(current.children[index] == null){
                return suggestions;
            }
            current = current.children[index];
        }
        getSuggestionRecursively(current, searchText, suggestions);
        return suggestions;
    }

    private void getSuggestionRecursively(TrieNode lastLetter, String s, List<String> suggestions){
        if(lastLetter == null){
            return;
        }
        if(lastLetter.eow){
            suggestions.add(s);
        }
        for(int i=0; i<26; i++){
            int alphabet = i + 'a';
            String charStr = String.valueOf((char) alphabet);
            getSuggestionRecursively(lastLetter.children[i], s+charStr, suggestions);
        }
    }
}

public class AutoCompletionLLD {
    public static void main(String[] args) {
        TrieUtil t = new TrieUtil();

        t.insert("pine");
        t.insert("pineapp");
        t.insert("pineapple");
        t.insert("pinterest");
        t.insert("in");
        t.insert("post");

        System.out.println(t.search("pine"));
    }
}
