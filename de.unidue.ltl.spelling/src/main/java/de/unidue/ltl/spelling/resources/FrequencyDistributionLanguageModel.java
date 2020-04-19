package de.unidue.ltl.spelling.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.dkpro.core.api.frequency.util.ConditionalFrequencyDistribution;
import org.dkpro.core.api.frequency.util.FrequencyDistribution;

// TODO: would preferably use CFD, but CFD does not implement serializable
public class FrequencyDistributionLanguageModel extends LanguageModelResource {

	ConditionalFrequencyDistribution<Integer, String> cfd;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map additionalParams)
			throws ResourceInitializationException {
		
		if (!super.initialize(aSpecifier, additionalParams)) {
			return false;
		}
		cfd = new ConditionalFrequencyDistribution<Integer, String>();

		FileInputStream fi = null;
		ObjectInputStream oi = null;
		File file = null;

//		for (String modelFile : modelFiles) {
			file = new File(modelFile);
			Integer condition = null;
			try {
				condition = Integer.parseInt(FilenameUtils.removeExtension(file.getName()));
				try {
					fi = new FileInputStream(modelFile);
					oi = new ObjectInputStream(fi);
					cfd.setFrequencyDistribution(condition, (FrequencyDistribution<String>) oi.readObject());
					getUimaContext().getLogger().log(Level.INFO,
							"Frequency distrbution '" + modelFile + "' succesfully deseralized and added to ConditionalFrequencyDistribution under condition "+condition+".");
				} catch (IOException e) {
					e.printStackTrace();
					getUimaContext().getLogger().log(Level.WARNING,
							"Unable to read frequency distribution from '" + modelFile + "'.");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					if (fi != null) {
						try {
							fi.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (oi != null) {
						try {
							oi.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
			} catch (NumberFormatException e) {
				getUimaContext().getLogger().log(Level.INFO,
						"Unable to reassemble ConditionalFrequencyDistribution. Files must be named <Integer-value>.ser, but you passed a File named "
								+ modelFile
								+ " not conforming to this whose name hence could not be parsed as Integer. It will not be included in your ConditionalFrequencyDistribution.");
			}
//		}
		return true;

	}

	@Override
	public double getFrequency(String[] ngram) {
		String ngramJoined = String.join(" ", ngram);
		int ngramSize = StringUtils.countMatches(ngramJoined, " ");
		double result = cfd.getCount(ngramSize, ngramJoined);
		if (result == 0) {
			result = 1.0 / cfd.getFrequencyDistribution(ngramSize).getN();
		}
		System.out.println("Looking up "+ngram);
		return result;
	}

}
