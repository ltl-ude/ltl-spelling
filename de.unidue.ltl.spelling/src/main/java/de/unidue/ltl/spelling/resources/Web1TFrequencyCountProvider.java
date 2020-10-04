package de.unidue.ltl.spelling.resources;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import org.dkpro.core.api.frequency.provider.FrequencyCountProviderBase;

import com.googlecode.jweb1t.JWeb1TIterator;
import com.googlecode.jweb1t.JWeb1TSearcher;

public class Web1TFrequencyCountProvider extends FrequencyCountProviderBase implements FrequencyCountProvider {

	JWeb1TSearcher searcher;
	String basePath;
	String language;

	public Web1TFrequencyCountProvider(String modelLocation, String language, int minNgramSize, int maxNgramSize) {
		basePath = modelLocation;
		this.language = language;
		try {
			searcher = new JWeb1TSearcher(new File(modelLocation), minNgramSize, maxNgramSize);
		} catch (IOException e) {
			System.err.println("Unable to read Web1T from " + modelLocation);
			e.printStackTrace();
		}
	}

	@Override
	public long getNrOfTokens() {
		return searcher.getNrOfNgrams(1);
	}

	@Override
	public long getNrOfNgrams(int n) {
		return searcher.getNrOfNgrams(n);
	}

	@Override
	public long getNrOfDistinctNgrams(int n) {
		return searcher.getNrOfDistinctNgrams(n);
	}

	@Override
	public Iterator<String> getNgramIterator(int n) throws IOException {
		return new JWeb1TIterator(basePath, n).getIterator();
	}

	@Override
	public String getLanguage() throws IOException {
		return this.language;
	}

	@Override
	protected long getFrequencyFromProvider(String phrase) throws IOException {
		return searcher.getFrequency(phrase);
	}
}