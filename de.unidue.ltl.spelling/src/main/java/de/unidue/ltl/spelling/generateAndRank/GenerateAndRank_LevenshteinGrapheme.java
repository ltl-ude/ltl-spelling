package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Generates and ranks candidates based on (weighted) levenshtein distance and
 * graphemes.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly" },
		// No real outputs, just SuggestedActions as entries to the SpellingAnomalies?
		outputs = { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction" })
public class GenerateAndRank_LevenshteinGrapheme extends CandidateGeneratorAndRanker {

	/**
	 * File containing tab-separated line-by-line entries of deletion costs for
	 * characters, e.g. "a\t 5". Entries must be lowercase.
	 */
	public static final String PARAM_WEIGHT_FILE_DELETION = "weightFileDeletion";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_DELETION, mandatory = false)
	protected String weightFileDeletion;

	/**
	 * File containing tab-separated line-by-line entries of insertion costs for
	 * characters, e.g. "a\t 5". Entries must be lowercase.
	 */
	public static final String PARAM_WEIGHT_FILE_INSERTION = "weightFileInsertion";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_INSERTION, mandatory = false)
	protected String weightFileInsertion;

	/**
	 * File containing tab-separated line-by-line entries of substitution costs
	 * between characters, e.g. "a\t b\t 5". Entries must be lowercase.
	 */
	public static final String PARAM_WEIGHT_FILE_SUBSTITUTION = "weightFileSubstitution";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_SUBSTITUTION, mandatory = false)
	protected String weightFileSubstitution;

	/**
	 * File containing tab-separated line-by-line entries of transposition costs
	 * between characters, e.g. "a\t b\t 5". Entries must be lowercase.
	 */
	public static final String PARAM_WEIGHT_FILE_TRANSPOSITION = "weightFileTransposition";
	@ConfigurationParameter(name = PARAM_WEIGHT_FILE_TRANSPOSITION, mandatory = false)
	protected String weightFileTransposition;

	/**
	 * Sets the distance to apply when no weights were supplied for a certain
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

	private Map<Character, Float> deletionMap;
	private Map<Character, Float> insertionMap;
	private Map<Character, Map<Character, Float>> substitutionMap;
	private Map<Character, Map<Character, Float>> transpositionMap;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionaries(dictionaries);

		deletionMap = readWeights(weightFileDeletion);
		insertionMap = readWeights(weightFileInsertion);
		substitutionMap = readWeights2D(weightFileSubstitution);
		transpositionMap = readWeights2D(weightFileTransposition);
		if (includeTransposition && weightFileTransposition != null) {
			includeTransposition = true;
			getContext().getLogger().log(Level.WARNING,
					"Transposition was not chosen to be included in GenerateAndRank_LevenshteinGrapheme, but you provided the file'"
							+ weightFileTransposition + "' with transposition weights. They will be included.");
		}

		System.out.println(deletionMap);
		System.out.println(insertionMap);
		System.out.println(substitutionMap);
		System.out.println(transpositionMap);
	}

	private Map<Character, Map<Character, Float>> readWeights2D(String weightFile) {
		if (weightFile != null) {
			Map<Character, Map<Character, Float>> weightMap = new HashMap<Character, Map<Character, Float>>();
			weightMap = new HashMap<Character, Map<Character, Float>>();

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					Map<Character, Float> currentCharacterMap = weightMap.get(weightEntry[0].charAt(0));
					if (currentCharacterMap == null) {
						weightMap.put(weightEntry[0].toLowerCase().charAt(0), new HashMap<Character, Float>());
						currentCharacterMap = weightMap.get(weightEntry[0].toLowerCase().charAt(0));
					}
					if (currentCharacterMap.get(weightEntry[1].toLowerCase().charAt(0)) == null) {
						currentCharacterMap.put(weightEntry[1].toLowerCase().charAt(0),
								Float.parseFloat(weightEntry[2]));
					} else {
						getContext().getLogger().log(Level.WARNING,
								"You provided two different weights for '" + weightEntry[0] + "' to '" + weightEntry[1]
										+ "' (" + currentCharacterMap.get(weightEntry[1].toLowerCase().charAt(0))
										+ " and " + weightEntry[2] + ") in File" + weightFile
										+ ". The former will be used.");
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

	private Map<Character, Float> readWeights(String weightFile) {
		if (weightFile != null) {
			Map<Character, Float> weightMap = new HashMap<Character, Float>();
			weightMap = new HashMap<Character, Float>();

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					Float currentWeight = weightMap.get(weightEntry[0].toLowerCase().charAt(0));
					if (currentWeight == null) {
						weightMap.put(weightEntry[0].toLowerCase().charAt(0), Float.parseFloat(weightEntry[1]));
					} else {
						// Warn that there is double info for this char
						getContext().getLogger().log(Level.WARNING,
								"You provided two different weights for '" + weightEntry[0] + "' (" + currentWeight
										+ " and " + weightEntry[1] + ") in File" + weightFile
										+ ". The former will be used.");
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

	// Assumes performing operations on 'wrong' to turn it into 'right'
	@Override
	protected float calculateCost(String wrong, String right) {
		
//		System.out.println("Calculating cost for "+wrong+" and "+right+".");

		float[][] distanceMatrix = new float[wrong.length() + 1][right.length() + 1];

		// Worst case: cost of starting from scratch: inserting all chars (weighted
		// accordingly)
		distanceMatrix[0][0] = 0;
		for (int i = 1; i <= wrong.length(); i++) {
			distanceMatrix[i][0] = distanceMatrix[i - 1][0] + getInsertionWeight(wrong.charAt(i - 1));
		}
		distanceMatrix[0][0] = 0;
		for (int j = 1; j <= right.length(); j++) {
			distanceMatrix[0][j] = distanceMatrix[0][j - 1] + getInsertionWeight(right.charAt(j - 1));
		}

		for (int wrongIndex = 1; wrongIndex <= wrong.length(); wrongIndex++) {
			for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {

				// INSERTION:
				float insertion = distanceMatrix[wrongIndex][rightIndex - 1]
						+ getInsertionWeight(right.charAt(rightIndex - 1));

				// DELETION:
				float deletion = distanceMatrix[wrongIndex - 1][rightIndex]
						+ getDeletionWeight(wrong.charAt(wrongIndex - 1));

				// SUBSTITUTION
				int charsAreDifferent = 1;
				if (wrong.charAt(wrongIndex - 1) == (right.charAt(rightIndex - 1))) {
					charsAreDifferent = 0;
				}
				float substitution = distanceMatrix[wrongIndex - 1][rightIndex - 1] + charsAreDifferent
						* getSubstitutionWeight(wrong.charAt(wrongIndex - 1), right.charAt(rightIndex - 1));

				// TRANSPOSITION
				if (includeTransposition && wrongIndex > 1 && rightIndex > 1
						&& lowercasedCharsAreEqual(wrong.charAt(wrongIndex - 1), right.charAt(rightIndex - 2))
						&& lowercasedCharsAreEqual(wrong.charAt(wrongIndex - 2), right.charAt(rightIndex - 1))) {
					float transposition = distanceMatrix[wrongIndex - 2][rightIndex - 2]
							+ getTranspositionWeight(wrong.charAt(wrongIndex - 2), wrong.charAt(wrongIndex - 1));
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
//		System.out.println("Distance between\t" + wrong + "\tand\t" + right + "\tis:\t"
//				+ distanceMatrix[wrong.length()][right.length()]);

		return distanceMatrix[wrong.length()][right.length()];
	}

	private float getInsertionWeight(char a) {
		
		if (insertionMap == null) {
			return defaultWeight;
		} else {
			try {
				char lowercased_a = Character.toLowerCase(a);
				return insertionMap.get(lowercased_a);
			} catch (NullPointerException e) {
//				System.out.println("No insertion weight found for char " + a + ". Applying default weight instead.");				
				return defaultWeight;
			}
		}
	}

	private float getDeletionWeight(char a) {
		if (deletionMap == null) {
			return defaultWeight;
		} else {
			try {
				char lowercased_a = Character.toLowerCase(a);
				return deletionMap.get(lowercased_a);
			} catch (NullPointerException e) {
//				System.out.println("No deletion weight found for char " + a + ". Applying default weight instead.");
				return defaultWeight;
			}
		}
	}

	private float getSubstitutionWeight(char a, char b) {
		if (substitutionMap == null) {
			float result = 0;
			if (!lowercasedCharsAreEqual(a, b)) {
				result += defaultWeight;
			}
			return result + compareCases(a, b);
		} else {
			try {
				char lowercased_a = Character.toLowerCase(a);
				char lowercased_b = Character.toLowerCase(b);
				return substitutionMap.get(lowercased_a).get(lowercased_b) + compareCases(a, b);
			} catch (NullPointerException e) {
//				System.out.println("No substition weight found for chars " + a + " and " + b
//						+ ". Applying default weight instead.");
				return defaultWeight + compareCases(a, b);
			}
		}
	}

	private float getTranspositionWeight(char a, char b) {
		if (transpositionMap == null) {
			return defaultWeight + compareCases(a, b);
		} else {
			try {
				char lowercased_a = Character.toLowerCase(a);
				char lowercased_b = Character.toLowerCase(b);
				return transpositionMap.get(lowercased_a).get(lowercased_b) + compareCases(a, b);
			} catch (NullPointerException e) {
//				System.out.println("No transposition weight found for chars " + a + " and " + b
//						+ ". Applying default weight instead.");
				return defaultWeight + compareCases(a, b);
			}
		}
	}

	private float compareCases(char a, char b) {

		if (Character.isLowerCase(a) == Character.isLowerCase(b)) {
			return 0.0f;
		} else {
			return 0.5f;
		}
	}

	private boolean lowercasedCharsAreEqual(char a, char b) {
		return Character.toLowerCase(a) == Character.toLowerCase(b);
	}

}