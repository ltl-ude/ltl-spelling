package de.unidue.ltl.spelling.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;

// TODO: would preferably use CFD, but CFD does not implement serializable
public class FrequencyDistributionLanguageModel extends LanguageModelResource {

	FrequencyDistribution<String> fd = null;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map additionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, additionalParams)) {
			return false;
		}
		FileInputStream fi = null;
		ObjectInputStream oi = null;
		try{
			fi = new FileInputStream(modelFile);
			oi = new ObjectInputStream(fi);
			fd = (FrequencyDistribution<String>) oi.readObject();
			getUimaContext().getLogger().log(Level.INFO,
					"Frequency distrbution '" + modelFile + "' was succesfully deseralized.");
//			Check how many entries of requested size are present in fd
//			getUimaContext().getLogger().log(Level.INFO, fd.getFrequencyDistribution(ngramSize).getKeys().size()+" ngrams of selected NGRAM_SIZE (" + ngramSize
//					+ ") are present in ConditionalFrequencyDistribution you passed.");
		}
		catch (IOException e) {
			e.printStackTrace();
			getUimaContext().getLogger().log(Level.WARNING,
					"Unable to read frequency distribution from '" + modelFile + "'.");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally {
			if(fi != null) {
				try {
					fi.close();
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
			if(oi != null) {
				try {
					oi.close();
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	// TODO: current implementation relies on the FD that was passed only containing ngrams of the requested size
	@Override
	public double getFrequency(String[] ngram) {
		String ngramJoined = String.join(" ", ngram);
		double result = fd.getCount(ngramJoined) / (double) fd.getN();
		if (result == 0) {
			result = 1.0 / fd.getN();
		}
		return result;
	}

}
