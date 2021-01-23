package de.unidue.ltl.spelling.resources;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import org.dkpro.core.api.frequency.provider.FrequencyCountProviderBase;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;

public class CFDFrequencyCountProvider extends FrequencyCountProviderBase implements FrequencyCountProvider {

	ConditionalFrequencyDistribution<Integer, String> cfd;
	String language;

	public CFDFrequencyCountProvider(ConditionalFrequencyDistribution<Integer, String> cfd, String language) {
		this.cfd = cfd;
		this.language = language;
	}

	@Override
	public long getNrOfTokens() throws IOException {
		try {
			return cfd.getFrequencyDistribution(1).getN();
		} catch (NullPointerException e) {
			return 0;
		}
	}

	@Override
	public long getNrOfNgrams(int n) throws IOException {
		try {
			return cfd.getFrequencyDistribution(n).getN();
		} catch (NullPointerException e) {
			System.err.println("There was an attempt to retrieve " + n + "-gram frequencies from the language model "
					+ this.getID() + ", but this model does not contain any n-grams of this size.");
			return 0;
		}
	}

	@Override
	public long getNrOfDistinctNgrams(int n) throws IOException {
		return cfd.getFrequencyDistribution(n).getB();
	}

	@Override
	public Iterator<String> getNgramIterator(int n) throws IOException {
		return cfd.getFrequencyDistribution(n).getKeys().iterator();
	}

	@Override
	public String getLanguage() throws IOException {
		return this.language;
	}

	@Override
	protected long getFrequencyFromProvider(String phrase) throws IOException {
		try {
			return cfd.getFrequencyDistribution(StringUtils.countMatches(phrase, " ")+1).getCount(phrase);
		} catch (NullPointerException e) {
			return -1;
		}
	}
}
