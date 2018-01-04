package dvolv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestSetText extends TestSet {

    private final static int factor = 4;

    String[] words = {
        "ant", 
        "ape", 
        "asp",
        "ass", 
        "bat", 
        "bee",
        "boa",
        "cat", 
        "cow", 
        "dog", 
        "eel",
        "elk",
        "emu",
        "fly",
        "fox", 
        "gnu", 
        "hog",
        "jay",
        "koi",
        "owl",
        "pig", 
        "ram", 
        "rat",
        "tit",
        "yak", 
    };
    private Set<String> wordsSet;
    private List<TestCase> posCases;
    
    public TestSetText() {
        name = "tlwords-1to" + factor + "-bal-regen";
        wordsSet = new HashSet<String>();
        posCases = new ArrayList<TestCase>();
        
        for (String word : words) {
            wordsSet.add(word);
            TestCase tc = caseFromWord(word);
            tc.addOutput("o", 1.0);
            tc.weight = (factor * 1.0);
            posCases.add(tc);
        }

        regen();
    }

    public void regen() {
        cases = new ArrayList<TestCase>();
        cases.addAll(posCases);

        for (int i=0; i<(posCases.size()*factor); i++) {
            String word = randomWord(3);
            if (wordsSet.contains(word)) continue;
            TestCase tc = caseFromWord(word);
            tc.addOutput("o", 0.0);
            tc.weight = 1.0;
            cases.add(tc);
        }
    }

    private TestCase caseFromWord(String word) {
        TestCase tc = new TestCase();
        List<Double> wordAsNums = w2d(word);
        for (int i=0; i<wordAsNums.size(); i++) {
            tc.addInput("ch"+i, wordAsNums.get(i));
        }
        return tc;
    }

    private String randomWord(int length) {
        int min = 'a';
        int max = 'z';
        StringBuilder s = new StringBuilder();
        for (int i=0; i<length; i++) {
            s.append((char)(DVMath.randInt(min, max)));
        }
        return s.toString();
    }

    public TestCase randomCase() {
        String word = randomWord(3);
        TestCase tc = caseFromWord(word);
        tc.readable = word;
        tc.addOutput("o", 1.0);
        return tc;
    }

    private List<Double> w2d(String word) {
        double min = a2i('a');
        double max = a2i('z');

        List<Double> letters = new ArrayList<Double>();
        for(char c : word.toCharArray()) { 
            double l = a2i(c);
            letters.add((l-min)/(max-min));
        }

        return letters;
    }

    private int a2i(char c) {
        return (int)(c);
    }

}
