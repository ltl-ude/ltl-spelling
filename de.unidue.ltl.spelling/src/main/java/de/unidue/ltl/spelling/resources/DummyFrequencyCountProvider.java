package de.unidue.ltl.spelling.resources;

import java.io.IOException;
import java.util.Iterator;

import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;

// Does not have any effect other than to satisfy the requirement of passing a resource

public class DummyFrequencyCountProvider implements FrequencyCountProvider{
	
	@Override
	public long getFrequency(String phrase) throws IOException {
		return 0;
	}

	@Override
	public double getProbability(String phrase) throws IOException {
		return 0;
	}

	@Override
	public double getLogProbability(String phrase) throws IOException {
		return 0;
	}

	@Override
	public long getNrOfTokens() throws IOException {
		return 0;
	}

	@Override
	public long getNrOfNgrams(int n) throws IOException {
		return 0;
	}

	@Override
	public long getNrOfDistinctNgrams(int n) throws IOException {
		return 0;
	}

	@Override
	public Iterator<String> getNgramIterator(int n) throws IOException {
		return null;
	}

	@Override
	public String getLanguage() throws IOException {
		return null;
	}

	@Override
	public String getID() {
		return "dummy";
	}

}
