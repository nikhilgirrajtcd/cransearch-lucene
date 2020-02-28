package com.cransearch.runner;

/**
 * Author - Nikhil Girraj
 * 
 */
public final class App {

    private App() {
        // Nothing much.
    }

    public static void main(final String[] args) throws Exception {
        try {
        	System.out.println("+ Begin.");
            
        	SearchEngine searchEngine = new SearchEngine("res/io", "index");
            searchEngine.BuildIndex();
            searchEngine.SearchIndex();
            
        	System.out.println("+++ End.");
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
}
