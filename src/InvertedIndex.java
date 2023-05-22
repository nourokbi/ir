import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class InvertedIndex {
    // Collection size
    int N = 10;

    private HashMap<String, DictEntry> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    // method to read 10 text files and build the inverted index
    public void buildIndex() {
        // iterate over each file
        for (int i = 1; i <= 10; i++) {
            String filename = "file" + i + ".txt";
            try (Scanner scanner = new Scanner(new File(filename))) {
                // iterate over each word in the current file
                while (scanner.hasNext()) {
                    String word = scanner.next();
                    word = word.toLowerCase();
                    // retrieve the dict-entry for the current word, or create a new entry if it
                    // doesn't exist
                    DictEntry entry = index.getOrDefault(word, new DictEntry());
                    entry.term_freq++;
                    // iterate over the posting list for the current entry to find the current
                    // document, or the end of the list
                    Posting posting = entry.pList;
                    Posting prev = null;
                    while (posting != null && posting.docId != i) {
                        prev = posting;
                        posting = posting.next;
                    }
                    // if the current document is not in the posting list, add it
                    if (posting == null) {
                        Posting newPosting = new Posting();
                        newPosting.docId = i;
                        // means that there is no elements in the posting list
                        if (prev == null) {
                            entry.pList = newPosting;
                        } else {
                            prev.next = newPosting;
                        }
                        entry.doc_freq++;
                    } else {
                        // if the current document is already in the posting list, dtf++
                        posting.dtf++;
                    }
                    // update the dictionary entry for the current word
                    index.put(word, entry);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // method to list all files that contain a given word
    public List<Integer> getFiles(String word) {
        List<Integer> files = new ArrayList<>();
        // retrieve the dictionary entry for the given word
        DictEntry entry = index.get(word);
        if (entry != null) {
            // iterate over the posting list for the given word and add each document to the
            // list
            Posting posting = entry.pList;
            while (posting != null) {
                files.add(posting.docId);
                posting = posting.next;
            }
        }
        return files;
    }

    // method to calculate cosine similarity between a query and files
    public Map<Integer, Double> CosineSimilarity(List<String> query) {
        Map<Integer, Double> cosineSimilarities = new HashMap<>();

        // calculate tf-idf scores for query terms
        Map<String, Double> queryScores = new HashMap<>();
        for (String term : query) {
            term = term.toLowerCase();
            double tf = TermFrequency(query, term);
            double idf = InverseDocumentFrequency(term);
            double tfidf = tf * idf;
            queryScores.put(term, tfidf);
        }

        // calculate cosine similarity for each file
        for (int i = 1; i <= 10; i++) {
            List<String> fileWords = getFileWords("file" + i + ".txt");
            double dotProduct = 0.0;
            double queryMagnitude = 0.0;
            double fileMagnitude = 0.0;

            for (String term : query) {
                double queryScore = queryScores.getOrDefault(term, 0.0);
                double fileScore = TermFrequency(fileWords, term) * InverseDocumentFrequency(term);
                dotProduct += queryScore * fileScore;
                queryMagnitude += queryScore * queryScore;
                fileMagnitude += fileScore * fileScore;
            }

            queryMagnitude = Math.sqrt(queryMagnitude);
            fileMagnitude = Math.sqrt(fileMagnitude);
            double cosineSimilarity = dotProduct / (queryMagnitude * fileMagnitude);
            cosineSimilarities.put(i, cosineSimilarity);
        }

        return cosineSimilarities;
    }

    // helper method to calculate term frequency
    private double TermFrequency(List<String> words, String term) {
        long count = words.stream().filter(t -> t.equalsIgnoreCase(term)).count();
        return (double) count;
    }

    // helper method to calculate inverse document frequency
    private double InverseDocumentFrequency(String term) {
        DictEntry entry = index.getOrDefault(term.toLowerCase(), new DictEntry());
        int docFrequency = entry.doc_freq;
        // int collectionSize = 10;
        return Math.log10(N / (double) docFrequency);
    }

    // helper method to get words from a file
    private List<String> getFileWords(String filename) {
        List<String> words = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNext()) {
                String word = scanner.next().toLowerCase();
                words.add(word);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return words;
    }

    public static void main(String[] args) {
        InvertedIndex index = new InvertedIndex();
        // build the inverted index
        index.buildIndex();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter a query (space-separated words): ");
            String queryStr = scanner.nextLine();
            List<String> query = Arrays.asList(queryStr.toLowerCase().split(" "));

            // compute cosine similarity between files and the query
            Map<Integer, Double> cosineSimilarities = index.CosineSimilarity(query);

            // create a list to store file rankings
            List<RankedFile> rankedFiles = new ArrayList<>();

            // assign rankings to files based on cosine similarity values
            for (int file = 1; file <= 10; file++) {
                double cosineSimilarity = cosineSimilarities.getOrDefault(file, 0.0);
                if (Double.isNaN(cosineSimilarity)) {
                    cosineSimilarity = 0.0;
                }
                rankedFiles.add(new RankedFile(file, cosineSimilarity));
            }

            // sort the ranked files based on cosine similarity values
            rankedFiles.sort(Comparator.comparingDouble(RankedFile::getCosineSimilarity).reversed());
            DecimalFormat df = new DecimalFormat("#.#####");
            if (rankedFiles.isEmpty()) {
                System.out.println("No files contain the query: " + queryStr);
            } else {
                System.out.println("Files that contain the query: " + queryStr);
                int rank = 1;
                for (RankedFile rankedFile : rankedFiles) {
                    System.out.println("Rank " + rank + ": file" + rankedFile.getFileId() + ".txt (Cosine Similarity: "
                            + df.format(rankedFile.getCosineSimilarity()) + ")");
                    rank++;
                }
            }
        }
    }
}