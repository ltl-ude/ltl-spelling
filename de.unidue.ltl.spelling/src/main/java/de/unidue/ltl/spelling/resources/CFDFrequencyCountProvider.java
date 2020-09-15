package de.unidue.ltl.spelling.resources;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;

public class CFDFrequencyCountProvider implements FrequencyCountProvider {

	ConditionalFrequencyDistribution<Integer, String> cfd;

	public CFDFrequencyCountProvider(ConditionalFrequencyDistribution<Integer, String> cfd) {
		this.cfd = cfd;
	}

	@Override
	public long getFrequency(String phrase) throws IOException {
		try {
			return cfd.getFrequencyDistribution(StringUtils.countMatches(phrase, " ")).getCount(phrase);
		} catch (NullPointerException e) {
			return -1;
		}
	}

	@Override
	public double getProbability(String phrase) throws IOException {
		int n = StringUtils.countMatches(phrase, " ");
		try {
			return (1.0 * cfd.getFrequencyDistribution(n).getCount(phrase)) / ((1.0) * getNrOfNgrams(n));
		} catch (NullPointerException e) {
			return -1;
		}
	}

	@Override
	public double getLogProbability(String phrase) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNrOfTokens() throws IOException {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		return cfd.toString();
	}

}
