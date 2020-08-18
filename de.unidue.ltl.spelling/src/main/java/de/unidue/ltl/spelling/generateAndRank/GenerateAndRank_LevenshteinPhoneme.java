package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.unidue.ltl.spelling.utils.PhonemeUtils;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Generates and ranks candidates based on (weighted) levenshtein distance
 * calculated on phonemes.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly" },
		// No real outputs, just SuggestedActions as entries to the SpellingAnomalies?
		outputs = { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction" })
public class GenerateAndRank_LevenshteinPhoneme extends CandidateGeneratorAndRanker {

	/**
	 * File containing tab-separated line-by-line entries of deletion costs for
	 * SAMPA symbols, e.g. "a\t 5".
	 */
	public static final String PARAM_WEIGHT_FILE_DELETION = "weightFileDeletion";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_DELETION, mandatory = false)
	protected String weightFileDeletion;

	/**
	 * File containing tab-separated line-by-line entries of insertion costs for
	 * SAMPA symbols, e.g. "a\t 5".
	 */
	public static final String PARAM_WEIGHT_FILE_INSERTION = "weightFileInsertion";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_INSERTION, mandatory = false)
	protected String weightFileInsertion;

	/**
	 * File containing tab-separated line-by-line entries of substitution costs
	 * SAMPA symbols, e.g. "a\t b\t 5".
	 */
	public static final String PARAM_WEIGHT_FILE_SUBSTITUTION = "weightFileSubstitution";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_SUBSTITUTION, mandatory = false)
	protected String weightFileSubstitution;

	/**
	 * File containing tab-separated line-by-line entries of transposition costs
	 * between SAMPA symbols, e.g. "a\t b\t 5".
	 */
	public static final String PARAM_WEIGHT_FILE_TRANSPOSITION = "weightFileTransposition";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_TRANSPOSITION, mandatory = false)
	protected String weightFileTransposition;

	/**
	 * Sets the weight to apply when no weights were supplied for a certain
	 * operation.
	 */
	public static final String PARAM_DEFAULT_WEIGHT = "defaultDistance";
	@ConfigurationParameter(name = PARAM_DEFAULT_WEIGHT, mandatory = true, defaultValue = "1.0")
	protected float defaultWeight;

	/**
	 * Whether to permit transposition as a modification operation, e.g. apply
	 * Damerau-Levenshtein distance as opposed to standard Levenshtein Distance.
	 */
	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "False")
	protected boolean includeTransposition;

	/**
	 * Files containing line-by-line tab-separated entries of grapheme and phoneme
	 * versions of dictionary words.
	 */
	public static final String PARAM_GRAPHEME_TO_PHONEME_DICT_FILES = "graphemeToPhonemeDictFiles";
	@ConfigurationParameter(name = PARAM_GRAPHEME_TO_PHONEME_DICT_FILES, mandatory = true)
	protected String[] graphemeToPhonemeDictFiles;

	private Map<String, Float> deletionMap;
	private Map<String, Float> insertionMap;
	private Map<String, Map<String, Float>> substitutionMap;
	private Map<String, Map<String, Float>> transpositionMap;

	private Map<String, String> graphemeToPhonemeMap;

	// To keep track of missing weights and alert user only once
	private Set<String> missingWeights = new HashSet<String>();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		graphemeToPhonemeMap = readG2PMap(graphemeToPhonemeDictFiles);
		sortedDictionary = fillDictionary(graphemeToPhonemeMap);

		deletionMap = readWeights(weightFileDeletion);
		insertionMap = readWeights(weightFileInsertion);
		substitutionMap = readWeights2D(weightFileSubstitution);
		if (!includeTransposition && weightFileTransposition != null) {
			getContext().getLogger().log(Level.WARNING,
					"Transposition was not chosen to be included in GenerateAndRank_LevenshteinGrapheme, but you provided the file'"
							+ weightFileTransposition + "' with transposition weights. They will not be included.");
		} else {
			transpositionMap = readWeights2D(weightFileTransposition);
		}

//		System.out.println(deletionMap);
//		System.out.println(insertionMap);
//		System.out.println(substitutionMap);
//		System.out.println(transpositionMap);
	}

	private Map<Integer, Set<String>> fillDictionary(Map<String, String> graphemeToPhoneme) {
		Map<Integer, Set<String>> dictionaryMap = new HashMap<Integer, Set<String>>();
		for (String word : graphemeToPhoneme.keySet()) {
			int lengthOfTranscriptionOfCurrentWord = graphemeToPhoneme.get(word).split(" ").length;
			Set<String> wordsOfThisLength = dictionaryMap.get(lengthOfTranscriptionOfCurrentWord);
			if (wordsOfThisLength == null) {
				dictionaryMap.put(lengthOfTranscriptionOfCurrentWord, new HashSet<String>());
				wordsOfThisLength = dictionaryMap.get(lengthOfTranscriptionOfCurrentWord);
			}
			wordsOfThisLength.add(word);
		}
		return dictionaryMap;
	}

	private Map<String, String> readG2PMap(String[] mapFiles) {

		Map<String, String> g2pMap = new HashMap<String, String>();
		BufferedReader br;

		for (String mapFile : mapFiles) {
			try {
				br = new BufferedReader(new FileReader(new File(mapFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] entry = line.split("\t");
					g2pMap.put(entry[0], entry[1]);
				}
				br.close();
			} catch (IOException e) {
				getContext().getLogger().log(Level.WARNING, "Error reading g2p map.");
				e.printStackTrace();
			}
		}
		return g2pMap;
	}

	private Map<String, Map<String, Float>> readWeights2D(String weightFile) {
		if (weightFile != null) {
			Map<String, Map<String, Float>> weightMap = new HashMap<String, Map<String, Float>>();

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					if (weightEntry.length != 3) {
						getContext().getLogger().log(Level.WARNING,
								"Tab-separated triples of character, character, and weight are expected, but file '"
										+ weightFile + "' contained the line '" + line + ", which will be ignored.");
						continue;
					}
					Float weight = 0.0f;
					try {
						weight = Float.parseFloat(weightEntry[2]);
					} catch (NumberFormatException e) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[2]
								+ "' for '" + weightEntry[0] + "' and '" + weightEntry[1]
								+ ", which cannot be parsed as a float. This entry of the weight file will be ignored.");
						continue;
					}

					Map<String, Float> currentMap = weightMap.get(weightEntry[0]);
					if (currentMap == null) {
						weightMap.put(weightEntry[0], new HashMap<String, Float>());
						currentMap = weightMap.get(weightEntry[0]);
					}
					if (currentMap.get(weightEntry[1]) == null) {
						currentMap.put(weightEntry[1], weight);
					} else {
						getContext().getLogger().log(Level.WARNING,
								"You provided two weights for '" + weightEntry[0] + "' to '" + weightEntry[1] + "' ("
										+ currentMap.get(weightEntry[1]) + " and " + weightEntry[2] + ") in File"
										+ weightFile + ". The former weight will be used.");
					}
				}
				br.close();
				return weightMap;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private Map<String, Float> readWeights(String weightFile) {
		if (weightFile != null) {
			Map<String, Float> weightMap = new HashMap<String, Float>();

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					if (weightEntry.length != 2) {
						getContext().getLogger().log(Level.WARNING,
								"Tab-separated pairs of characters and weights are expected, but file '" + weightFile
										+ "' contained the line '" + line + ", which will be ignored.");
						continue;
					}

					Float weightFromFile = 0.0f;
					try {
						weightFromFile = Float.parseFloat(weightEntry[1]);
					} catch (NumberFormatException e) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[1]
								+ "' for '" + weightEntry[0]
								+ "', which cannot be parsed as a float. This entry of the weight file will be ignored.");
						continue;
					}
					Float weightInMap = weightMap.get(weightEntry[0]);
					if (weightInMap == null) {
						weightMap.put(weightEntry[0], weightFromFile);
					} else {
						// Warn that there is double info for this char
						getContext().getLogger().log(Level.WARNING,
								"You provided two weights for '" + weightEntry[0] + "' (" + weightInMap + " and "
										+ weightEntry[1] + ") in File" + weightFile
										+ ". The former weight will be used.");
					}
				}
				br.close();
				return weightMap;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected String getStringToCorrectFromAnomaly(SpellingAnomaly anomaly) {
		return PhonemeUtils.getPhoneticTranscription(anomaly.getCoveredText(), language);
	}

	@Override
	protected int getLengthOfMisspelling(String misspelling) {
		return misspelling.split(" ").length;
	}

	// Assumes performing operations on 'wrong' to turn it into 'right'
	// Called with phonetic transcription for 'wrong' as per overriden method
	// 'getStringToCorrectFromAnomaly'
	@Override
	protected float calculateCost(String wrong, String right) {

		right = graphemeToPhonemeMap.get(right);
		String[] misspellingArray = wrong.split(" ");
		String[] correctionArray = right.split(" ");

//		System.out.println("Calculating cost for "+wrong+" and "+right);

		float[][] distanceMatrix = new float[misspellingArray.length + 1][correctionArray.length + 1];

		// Worst case: cost of starting from scratch: inserting all chars (weighted
		// accordingly)
		distanceMatrix[0][0] = 0;
		for (int i = 1; i <= misspellingArray.length; i++) {
			distanceMatrix[i][0] = distanceMatrix[i - 1][0] + getInsertionWeight(misspellingArray[i - 1]);
		}

		for (int j = 1; j <= correctionArray.length; j++) {
			distanceMatrix[0][j] = distanceMatrix[0][j - 1] + getInsertionWeight(correctionArray[j - 1]);
		}

		for (int wrongIndex = 1; wrongIndex <= misspellingArray.length; wrongIndex++) {
			for (int rightIndex = 1; rightIndex <= correctionArray.length; rightIndex++) {

				// INSERTION:
				float insertion = distanceMatrix[wrongIndex][rightIndex - 1]
						+ getInsertionWeight(correctionArray[rightIndex - 1]);

				// DELETION:
				float deletion = distanceMatrix[wrongIndex - 1][rightIndex]
						+ getDeletionWeight(misspellingArray[wrongIndex - 1]);

				// SUBSTITUTION
				int charsAreDifferent = 1;
				if (misspellingArray[wrongIndex - 1].equals(correctionArray[rightIndex - 1])) {
					charsAreDifferent = 0;
				}
				float substitution = distanceMatrix[wrongIndex - 1][rightIndex - 1] + charsAreDifferent
						* getSubstitutionWeight(misspellingArray[wrongIndex - 1], correctionArray[rightIndex - 1]);

				// TRANSPOSITION
				if (includeTransposition && wrongIndex > 1 && rightIndex > 1
						&& (misspellingArray[wrongIndex - 1].equals(correctionArray[rightIndex - 2]))
						&& (misspellingArray[wrongIndex - 2].equals(correctionArray[rightIndex - 1]))) {
					float transposition = distanceMatrix[wrongIndex - 2][rightIndex - 2] + getTranspositionWeight(
							misspellingArray[wrongIndex - 2], misspellingArray[wrongIndex - 1]);
					distanceMatrix[wrongIndex][rightIndex] = Math.min(deletion,
							Math.min(insertion, Math.min(substitution, transposition)));
				} else {
					distanceMatrix[wrongIndex][rightIndex] = Math.min(deletion, Math.min(insertion, substitution));
				}
			}
		}

		// To visualize cost matrix
//		for (int i = 0; i < wrong.length() + 1; i++) {
//			for (int j = 0; j < right.length() + 1; j++) {
//
//				System.out.print(distanceMatrix[i][j] + "\t");
//
//			}
//			System.out.println();
//		}
//		System.out.println("Cost from \t" + wrong + "\tto\t" + right + "\tis:\t"
//				+ distanceMatrix[misspellingArray.length][correctionArray.length]);

		return distanceMatrix[misspellingArray.length][correctionArray.length];
	}

	private float getInsertionWeight(String stringToInsert) {

		if (insertionMap == null) {
			return defaultWeight;
		} else {
			try {
				return insertionMap.get(stringToInsert);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(stringToInsert + "ins")) {
					missingWeights.add(stringToInsert + "ins");
					getContext().getLogger().log(Level.INFO, "No insertion weight was provided for '" + stringToInsert
							+ "'. Applying default weight instead.");
				}
				return defaultWeight;
			}
		}
	}

	private float getDeletionWeight(String stringToDelete) {
		if (deletionMap == null) {
			return defaultWeight;
		} else {
			try {
				return deletionMap.get(stringToDelete);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(stringToDelete + "del")) {
					missingWeights.add(stringToDelete + "del");
					getContext().getLogger().log(Level.INFO, "No deletion weight was prodived for '" + stringToDelete
							+ "'. Applying default weight instead.");
				}
				return defaultWeight;
			}
		}
	}

	private float getSubstitutionWeight(String toReplace, String replacement) {
		if (toReplace.equals(replacement)) {
			return 0;
		}
		if (substitutionMap == null) {
			return defaultWeight;
		} else {
			try {
				return substitutionMap.get(toReplace).get(replacement);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(toReplace + "sub")
						&& !missingWeights.contains(toReplace + replacement + "sub")) {
					if (substitutionMap.get(toReplace) == null) {
						missingWeights.add(toReplace + "sub");
						getContext().getLogger().log(Level.INFO, "No substition weights were provided for '" + toReplace
								+ ". Applying default weight instead.");
					} else {
						missingWeights.add(toReplace + replacement + "sub");
						getContext().getLogger().log(Level.INFO,
								"No substition weight was provided for replacement of '" + toReplace + "' with '"
										+ replacement + "'. Applying default weight instead.");
					}
				}
				return defaultWeight;
			}
		}
	}

	private float getTranspositionWeight(String leftString, String rightString) {
		if (transpositionMap == null) {
			return defaultWeight;
		} else {
			try {
				return transpositionMap.get(leftString).get(rightString);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(leftString + "tran")
						&& !missingWeights.contains(leftString + rightString + "tran")) {
					if (transpositionMap.get(leftString) == null) {
						missingWeights.add(leftString + "tran");
						getContext().getLogger().log(Level.INFO, "No transposition weights were provided for '"
								+ leftString + "'. Applying default weight instead.");
					} else {
						missingWeights.add(leftString + rightString + "tran");
						getContext().getLogger().log(Level.INFO, "No transposition weight was provided for '"
								+ leftString + "' and '" + rightString + "'. Applying default weight instead.");
					}
				}
				return defaultWeight;
			}
		}
	}
}