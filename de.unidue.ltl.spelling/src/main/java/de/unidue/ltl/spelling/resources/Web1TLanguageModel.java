package de.unidue.ltl.spelling.resources;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import com.googlecode.jweb1t.JWeb1TSearcher;

public class Web1TLanguageModel extends LanguageModelResource {

	private JWeb1TSearcher web1tSearcher;
//	private JWeb1TIterator web1tIterator;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map additionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, additionalParams)) {
			return false;
		}
//        System.out.println("Initialize web1t LM from: "+modelFile);
		try {
			// TODO: fix index files to support this
			web1tSearcher = new JWeb1TSearcher(new File(modelFile), 1, 5);
		} catch (IOException e) {
			e.printStackTrace();
		}
//        System.out.println("unigram count: "+web1tSearcher.getNrOfDistinctNgrams(1));
		return true;
	}

	public float getFrequency(String[] ngram) {
		String ngramToQuery = String.join(" ", ngram);
		float count = 0.0f;
		try {
			count = (float) (web1tSearcher.getFrequency(ngramToQuery) * 1.0);
			System.out.println("Frequency of " + ngramToQuery + ": " + count);
			// TODO: This returns -1, something must be wrong with the web1t index files
			// (ENGLISH)
//					/web1tSearcher.getNrOfDistinctNgrams(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO: never return 0; set nonexisting ngrams to 1/ngramCount instead
		if (count == 0.0) {
			count = 1 / 10000;
		}
		return count;
	}
}