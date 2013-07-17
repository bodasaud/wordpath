
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;


public class WordPathTest {

    @Test
    public void testIthCharRemovedFirst() {
        assertEquals("even", WordPath.ithCharRemoved("seven", 0));
    }
    
    @Test
    public void testIthCharRemovedLast() {
        assertEquals("seve", WordPath.ithCharRemoved("seven", 4));
    }
    
    @Test
    public void testIthCharRemovedMiddle() {
        assertEquals("seen", WordPath.ithCharRemoved("seven", 2));
    }
    
    @Test
    public void testEdgeInfo() {
        List<String> words = new ArrayList<String>();
        words.add("smart");
        words.add("start");
        words.add("seven");
        
        Map<String, List<String>>[] edgeInfo = WordPath.getEdgeInfo(words);
        assertEquals(5, edgeInfo.length);
        
        // Test we have exactly the edges that we expect
        String[][] expectedKeys = new String[][]{
            new String[]{"mart", "tart", "even"},
            new String[]{"sart", "sart", "sven"},
            new String[]{"smrt", "strt", "seen"},
            new String[]{"smat", "stat", "sevn"},
            new String[]{"smar", "star", "seve"},
        };
        for (int i = 0; i < edgeInfo.length; i++) {
            Set<String> expected = new HashSet<String>();
            expected.addAll(Arrays.asList(expectedKeys[i]));
            assertEquals(expected, edgeInfo[i].keySet());
        }
        
        // Test that the edges connect the vertices we expect
        for (int i = 0; i < edgeInfo.length; i++) {
            for (String key : edgeInfo[i].keySet()) {
                if (i != 1 || !key.equals("sart")) {
                    assertEquals(1, edgeInfo[i].get(key).size());
                } else {
                    List<String> vertices = edgeInfo[1].get("sart");
                    assertEquals(2, vertices.size());
                    assertEquals(true, vertices.contains("smart"));
                    assertEquals(true, vertices.contains("start"));
                }
            }
        }
    }
    
    private static BufferedReader readDictWords(String filename) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
    }
    
    @Test
    public void testExample() throws IOException {
        BufferedReader br = readDictWords("/Users/tmulcahy/Documents/workspace/TalkoProblem/five-letter-words.txt");
        try {
            List<String> wordList = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                wordList.add(line);
            }
                    
            List<String> wordPath = WordPath.findShortestPath("smart", "brain", wordList);
            List<String> expected = new ArrayList<String>(Arrays.asList(new String[] {
                    "smart", "slart", "slait", "slain", "blain", "brain"
            }));
            assertEquals(expected, wordPath);
        } finally {
            br.close();
        }
    }
}
