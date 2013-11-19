import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;

import com.aliasi.corpus.ObjectHandler;
import com.aliasi.corpus.Corpus;

import com.aliasi.io.FileLineReader;

import com.aliasi.tokenizer.Tokenizer;

import com.aliasi.util.Arrays;
import com.aliasi.util.ObjectToSet;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TwentyNewsgroupsCorpus 
    extends Corpus<ObjectHandler<Classified<CharSequence>>> {
    
    final Map<String,String[]> mGoldCatToTexts;
    final Map<String,String[]> mTrainingCatToTexts;
    final Map<String,String[]> mTestCatToTexts;
    int mMaxSupervisedInstancesPerCategory = 1;

    public TwentyNewsgroupsCorpus(File path) throws IOException {
        File goldDir = new File(path,"gold");
        File trainDir = new File(path,"train");
        File testDir = new File(path,"test");
        mGoldCatToTexts = read(goldDir);
        mTrainingCatToTexts = read(trainDir);
        mTestCatToTexts = read(testDir);
    }

    public Set<String> categorySet() {
        return mTrainingCatToTexts.keySet();
    }
                                 
    public void permuteInstances(Random random) {
        for (String[] xs : mTrainingCatToTexts.values())
            Arrays.permute(xs,random);
        for (String[] xs : mGoldCatToTexts.values())
            Arrays.permute(xs,random);
    }

    public void setMaxSupervisedInstancesPerCategory(int max) {
        mMaxSupervisedInstancesPerCategory = max;
    }


    public void visitTrain(ObjectHandler<Classified<CharSequence>> handler) {
        visit(mTrainingCatToTexts,handler,mMaxSupervisedInstancesPerCategory);
    }
    
    public void visitGold(ObjectHandler<Classified<CharSequence>> handler) {
        visit(mGoldCatToTexts,handler,Integer.MAX_VALUE);
    }

    public void visitTest(ObjectHandler<Classified<CharSequence>> handler) {
        visit(mTestCatToTexts,handler,Integer.MAX_VALUE);
    }

    public Corpus<ObjectHandler<CharSequence>> unlabeledCorpus() {
        return new Corpus<ObjectHandler<CharSequence>>() {
            public void visitTest(ObjectHandler<CharSequence> handler) {
                throw new UnsupportedOperationException();
            }
            public void visitTrain(ObjectHandler<CharSequence> handler) {
                for (String[] texts : mTrainingCatToTexts.values())
                    for (int i = mMaxSupervisedInstancesPerCategory; 
                         i < texts.length; 
                         ++i)
                        handler.handle(texts[i]);
            }
        };
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int totalTrain = 0;
        int totalGold = 0;
        int totalTest = 0;
        for (String cat : new TreeSet<String>(mTrainingCatToTexts.keySet())) {
            sb.append(cat); 
            int gold = mGoldCatToTexts.get(cat).length;
            int train = mTrainingCatToTexts.get(cat).length;
            int test = 0;
            try {
                test = mTestCatToTexts.get(cat).length;
            } catch (Exception e) {
                //Nothing
            }
            totalGold += gold;
            totalTrain += train;
            totalTest += test;
            sb.append(" #gold=" + gold);
            sb.append(" #train=" + train);
            sb.append(" #test=" + test);
            sb.append('\n');
        }
        sb.append("TOTALS: #gold=" + totalGold
                  + " #train=" + totalTrain
                  + " #test=" + totalTest
                  + " #combined=" + (totalGold + totalTrain + totalTest));
        sb.append('\n');
        return sb.toString();
    }
    
    public int getTestCount(String cat){
        int test = 0;
        try {
            test = mTestCatToTexts.get(cat).length;
        } catch (Exception e) {
            //Nothing
        }
        return test;
    }

    private static Map<String,String[]> read(File dir) 
        throws IOException {
        ObjectToSet<String,String> catToTexts 
            = new ObjectToSet<String,String>();
        for (File catDir : dir.listFiles()) {
            String cat = catDir.getName();
            for (File file : catDir.listFiles()) {
                String[] lines 
                    = FileLineReader.readLineArray(file,"ISO-8859-1");
                Set<String> texts = new HashSet<String>(java.util.Arrays.asList(lines)); 
                catToTexts.addMembers(cat,texts);
            }
        }
        Map<String,String[]> map = new HashMap<String,String[]>();
        for (Map.Entry<String,Set<String>> entry : catToTexts.entrySet())
            map.put(entry.getKey(),
                    entry.getValue().toArray(new String[0]));
        return map;
    }

    private static void visit(Map<String,String[]> catToItems,
                              ObjectHandler<Classified<CharSequence>> handler,
                              int maxItems) {
        for (Map.Entry<String,String[]> entry : catToItems.entrySet()) {
            String cat = entry.getKey();
            Classification c = new Classification(cat);
            String[] texts = entry.getValue();
            for (int i = 0; i < maxItems && i < texts.length; ++i) {
                Classified<CharSequence> classifiedText
                    = new Classified<CharSequence>(texts[i],c);
                handler.handle(classifiedText);
            }
        }
    }
    

}