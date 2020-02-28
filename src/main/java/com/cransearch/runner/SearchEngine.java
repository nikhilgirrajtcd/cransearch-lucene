package com.cransearch.runner;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.MultiSimilarity;

public class SearchEngine {
    public String indexLocation;
    public String documentsLocation;

    public SearchEngine(String documentsDirectory, String indexDirectory) {
        this.indexLocation = indexDirectory;
        this.documentsLocation = documentsDirectory;
    }

    public Analyzer getAnalyzer() {
        CharArraySet caset = CharArraySet.copy(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        // stop words from NLTK library
        for (String s : new String[] { "i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "you're",
                "you've", "you'll", "you'd", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
                "she", "she's", "her", "hers", "herself", "it", "it's", "its", "itself", "they", "them", "their",
                "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "that'll", "these", "those",
                "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does",
                "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of",
                "at", "by", "for", "with", "about", "between", "into", "through", "during", "before", "after", "above",
                "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further",
                "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
                "more", "most", "other", "some", "such", "only", "own", "same", "so", "than", "too", "very", "s", "t",
                "can", "will", "just", "don", "don't", "should", "should've", "now", "d", "ll", "m", "re", "ve" }) {
            caset.add(s);
        }
       
        EnglishAnalyzer analyzer = new EnglishAnalyzer(caset);
        return analyzer;
    }

    public MultiSimilarity getSimilarity() {

        Similarity[] similarities = { new ClassicSimilarity(), new BM25Similarity()
        		// , new AxiomaticF1LOG() // reduces map value
                // , new BooleanSimilarity(), // reduces map value
                // , new LMJelinekMercerSimilarity(0.2f) // reduces map value

                // new LMDirichletSimilarity() // reduces map value
        };
        return new MultiSimilarity(similarities);
    }

    public void BuildIndex() throws IOException {
        // Basic outline of the process -
    	// Create Analyzer - for ex. a StandardAnalyzer
        // Open a Directory for index
        // Create Index writer config
        // Set up config, open
        // Create index writer
        // Create doc and add fields
        // Add docs to indexwriter 
        // close index writer and doc
    	
        final String cranDocsFilePath = documentsLocation + "/cran.all.1400";
        System.out.println("Reading the master document - " + cranDocsFilePath);

        final Analyzer azer = getAnalyzer();
        try {
            Directory dir = FSDirectory.open(Paths.get(this.indexLocation));
            final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(azer);
            indexWriterConfig.setOpenMode(OpenMode.CREATE);

            indexWriterConfig.setSimilarity(getSimilarity());

            final IndexWriter indexWriter = new IndexWriter(dir, indexWriterConfig);

            // documents
            System.out.print("Documents to index - ");
            final ArrayList<DocumentFragment> docFrags = getDocumentFragments(cranDocsFilePath);

            // index into separate fields and store the doc in a list
            final ArrayList<Document> documents = new ArrayList<Document>();
            for (final DocumentFragment docFrag : docFrags) {
                final Document doc = new Document();
                doc.add(new TextField("title", docFrag.Title, Field.Store.NO));
                doc.add(new TextField("author", docFrag.Author, Field.Store.NO));
                doc.add(new StringField("bib", docFrag.Biblio, Field.Store.NO));
                doc.add(new TextField("content", docFrag.Text, Field.Store.NO));
                doc.add(new StringField("filename", String.valueOf(docFrag.DocIndex), Field.Store.YES));
                documents.add(doc);
            }

            // add all docs and close index
            indexWriter.addDocuments(documents);
            indexWriter.close();
            dir.close();
            System.out.println("Indexed.");
        } catch (final Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public void SearchIndex() {
        try {
            // configurations
            final String queriesFile = this.documentsLocation + "/cran.qry";
            final String outputFile= this.documentsLocation + "/cranqrel-output";
            final int nResults = 1000;
            final FileWriter fileWriter = new FileWriter(outputFile);// +
            // Instant.now().getEpochSecond());

            System.out.print("Search queries - ");
            ArrayList<DocumentFragment> allQueries = getDocumentFragments(queriesFile);

            // Open the folder that contains our search index
            final Directory directory = FSDirectory.open(Paths.get(this.indexLocation));

            
            final DirectoryReader ireader = DirectoryReader.open(directory);
            final IndexSearcher isearcher = new IndexSearcher(ireader);
            
            // common similarity and analyzer
            isearcher.setSimilarity(getSimilarity());

            // must be the same as the one used when creating the index
            final Analyzer analyzer = getAnalyzer();

            // single field query parser
            // QueryParser queryParser = new QueryParser("content", analyzer);

            // multi-field Query parser, outperforms the single one
            String[] fieldsToSearchOver = { "content", "title", "author" };
            HashMap<String, Float> fieldBoostsMap = new HashMap<>();
            fieldBoostsMap.put("content", 5f);
            fieldBoostsMap.put("title", 2f);
            fieldBoostsMap.put("author", 1f);
            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchOver, analyzer, fieldBoostsMap);

            for (int iQueryNum = 0; iQueryNum < allQueries.size(); iQueryNum++) {
                DocumentFragment queryDocument = allQueries.get(iQueryNum);
                String queryString = QueryParser.escape(queryDocument.Text.trim());
                if (queryString.length() > 0) {

                    final Query query = queryParser.parse(queryString);
                    final ScoreDoc[] hits = isearcher.search(query, nResults).scoreDocs;

                    for (int i = 0; i < hits.length; i++) {
                        final Document hitDoc = isearcher.doc(hits[i].doc);
                        fileWriter.write(
                                (iQueryNum + 1) + " 0 " + hitDoc.get("filename") + " 0 " + hits[i].score + " Mark1\n");
                    }
                }
            }

            // close things
            ireader.close();
            directory.close();

            // close the qrel file
            fileWriter.close();
            System.out.println("QRel file generated.");
            System.out.println("Qrel output in file - " + outputFile);
            
        } catch (final Exception ex) {
            System.out.println(ex.toString());
        }

    }

    private static ArrayList<DocumentFragment> getDocumentFragments(final String pathToTheAllDocsFile) {

        final ArrayList<DocumentFragment> documentFragments = new ArrayList<DocumentFragment>();

        try {
            final String fileContent = new String(Files.readAllBytes(Paths.get(pathToTheAllDocsFile)));

            final String[] docsLines = fileContent.split("\n");
            DocumentFragment docFrag = null;
            Character stopCharacter = null;
            String section = "";
            String line = "";
            for (int i = 0; i < docsLines.length; i++) {
                line = docsLines[i];
                if (line.trim().length() > 0) {
                    if (line.charAt(0) == '.') {
                        stopCharacter = line.charAt(1);
                        if (stopCharacter == 'I') {
                            if (docFrag != null) {
                                docFrag.Text = section;
                                section = "";
                                documentFragments.add(docFrag);
                            }
                            docFrag = new DocumentFragment();
                            docFrag.DocIndex = Integer.parseInt(line.substring(2).trim());

                        } else if (stopCharacter == 'A') {
                            docFrag.Title = section;
                            section = "";
                        } else if (stopCharacter == 'B') {
                            docFrag.Author = section;
                            section = "";
                        } else if (stopCharacter == 'W') {
                            docFrag.Biblio = section;
                            section = "";
                        }
                    } else {
                        section += " " + line;
                    }
                }
            }

            if (docFrag != null) {
                docFrag.Text = section;
                section = "";
                documentFragments.add(docFrag);
            }

            System.out.print("Parsed. ");

        } catch (final Exception ex) {
            System.out.println(ex.toString());
        }
        return documentFragments;
    }
}

// A class to store the contents of a query or a document temporarily 
class DocumentFragment {
    public int DocIndex;
    public String Title;
    public String Author;
    public String Biblio;
    public String Text;
}