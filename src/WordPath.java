import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


// Given two five letter words, A and B, and a dictionary of five letter words,
// find a shortest transformation from A to B, such that only one letter can be
// changed at a time and all intermediate words in the transformation must
// exist in the dictionary.
// For example, if A and B are "smart" and "brain", the result may be:
//
// smart
//      start
//      stark
//      stack
//      slack
//      black
//      blank
//      bland
//      brand
//      braid
// brain
//
// Your implementation should take advantage of multiple CPU cores. Please also
// include test cases against your algorithm.
//


public class WordPath {
    public static final int WORD_LENGTH = 5;
    
    private WordPath() {} // All statics
    
    /**
     * Returns the shortest path from src to dst, using only words from
     * wordList along the way, or null if no such path exists
     */
    public static List<String> findShortestPath(
            String src, String dst, List<String> wordList) {
        long startMillis = System.currentTimeMillis();
        
        // Algorithm overview: We observe that the problem may be viewed as a graph.
        // Each word is a vertex and words with 4 letters in common in the same place
        // are connected by an edge. We can find the shortest path (if one exists) by
        // doing a breadth first traversal. An equivalent way of thinking about the
        // problem is that Djikstra's algorithm can be reduced to a breadth-first
        // traversal when all edges have the same weight.
        Map<String, List<String>>[] edgeInfo = getEdgeInfo(wordList);
        
        // Map from each vertex to the previous vertex that led to it. We also
        // use this to keep track of the nodes we've visited.
        Map<String, String> previousVertexMap = new HashMap<String, String>();
        
        List<String> currentVertices;
        List<String> nextVertices = new ArrayList<String>();
        nextVertices.add(src);
        
        previousVertexMap.put(src, null);
        
        while (!nextVertices.isEmpty()) {
            currentVertices = nextVertices;
            nextVertices = new ArrayList<String>();
            for (String vertex : currentVertices) {
                for (String adjacent : getAdjacentVertices(edgeInfo, vertex)) {
                    if (previousVertexMap.containsKey(adjacent)) {
                        continue;
                    }
                    nextVertices.add(adjacent);
                    previousVertexMap.put(adjacent, vertex);
                    if (adjacent.equals(dst)) {
                        System.out.println("bfs: " + (System.currentTimeMillis() - startMillis));
                        return reconstructPath(previousVertexMap, dst);
                    }
                }
            }
        }
        System.out.println("bfs: " + (System.currentTimeMillis() - startMillis));
        return null;
    }
    
    /**
     * Given the mapping of vertex to its parent in the BFS, reconstruct the path from src to dst.
     */
    static List<String> reconstructPath(Map<String, String> previousVertexMap, String dst) {
        ArrayList<String> backwardsPath = new ArrayList<String>();
        String current = dst;
        while (previousVertexMap.containsKey(current)) {
            backwardsPath.add(current);
            current = previousVertexMap.get(current);
        }
        
        Collections.reverse(backwardsPath);
        return backwardsPath; // backwardsPath is now forwards
    }
    
    /**
     * Encode the edge info for the given word list into five maps, one for each
     * character that is changed.
     */
    static Map<String, List<String>>[] getEdgeInfo(final List<String> wordList) {
        long startMillis = System.currentTimeMillis();
        
        // I'm using ConcurrentHashMap because I need putIfAbsent
        @SuppressWarnings("unchecked")
        final ConcurrentHashMap<String, List<String>>[] edgeInfo = new ConcurrentHashMap[WORD_LENGTH];
        
        for (int i = 0; i < edgeInfo.length; i++) {
            edgeInfo[i] = new ConcurrentHashMap<String, List<String>>();
        }
        
        // One thread per core
        int numProcs = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numProcs);
        
        int workSize = (wordList.size() / numProcs) + 1;
        
        int start = 0;
        while (start < wordList.size()) {
            final int finalStart = start;
            final int finalEnd = Math.min(wordList.size(), finalStart + workSize);
            start = finalEnd;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // Keep around an empty list to avoid creating garbage every time
                    // putIfAbsent doesn't do anything.
                    List<String> emptyVertices = Collections.synchronizedList(new ArrayList<String>());
                    
                    for (int i = finalStart; i < finalEnd; i++) {
                        String word = wordList.get(i);
                        for (int j = 0; j < WORD_LENGTH; j++) {
                            String edge = ithCharRemoved(word, j);
                            edgeInfo[j].putIfAbsent(edge, emptyVertices);
                            List<String> vertices = edgeInfo[j].get(edge);
                            if (vertices == emptyVertices) {
                                // Ours got put in - need a new empty list
                                emptyVertices = Collections.synchronizedList(new ArrayList<String>());
                            }
    
                            vertices.add(word);
                        }
                    }
                }
            });
        }
        executorService.shutdown(); // Previously submitted tasks will be completed.
        try {
            if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Failed to terminate.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("edge gen: " + (System.currentTimeMillis() - startMillis));
        
        return edgeInfo;
    }
    
    /**
     * Return the given string without the ith character.
     */
    static String ithCharRemoved(String src, int i) {
        return src.substring(0, i) + src.substring(i+1);
    }
    
    /**
     * Given the edge info encoded as described in getEdgeInfo, gather a list of all
     * vertices adjacent to the current vertex.
     */
    static List<String> getAdjacentVertices(Map<String, List<String>>[] edgeInfo, String vertex) {
        // TODO: Inefficient - could avoid garbage creation by using guava constructs
        List<String> adjacentVertices = new ArrayList<String>();
        for (int i = 0; i < WORD_LENGTH; i++) {
            for (String adjacent : edgeInfo[i].get(ithCharRemoved(vertex, i))) {
                if (!adjacent.equals(vertex)) {
                    adjacentVertices.add(adjacent);
                }
            }
        }
        
        return adjacentVertices;
    }
}
