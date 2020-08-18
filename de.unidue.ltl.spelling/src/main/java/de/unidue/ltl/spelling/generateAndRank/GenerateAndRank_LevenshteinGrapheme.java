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

import org.apache.commons.lang3.StringUtils;
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
	 * The dictionaries based on which to generate the correction candidates.
	 */
	public static final String PARAM_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_DICTIONARIES, mandatory = true)
	protected String[] dictionaries;

	/**
	 * Whether to process everything lowercased
	 */
	public static final String PARAM_LOWERCASE = "lowercase";
	@ConfigurationParameter(name = PARAM_LOWERCASE, mandatory = true, defaultValue = "False")
	protected boolean lowercase;

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
	 * Value to add if a lowercased character is to be replaced by an uppercased
	 * one. (Only applies if lowercase = false)
	 */
	public static final String PARAM_CAPITALIZATION_PENALTY = "capitalizationPenalty";
	@ConfigurationParameter(name = PARAM_CAPITALIZATION_PENALTY, mandatory = true, defaultValue = "0.5")
	protected float capitalizationPenalty;

	/**
	 * Sets the distance to apply when no weights were supplied for a certain
	 * operation.
	 */
	public static final String PARAM_DEFAULT_WEIGHT = "defaultWeight";
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

	// To keep track of missing weights and alert user only once
	private Set<String> missingWeights = new HashSet<String>();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionaries(dictionaries);

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

	private Map<Character, Map<Character, Float>> readWeights2D(String weightFile) {
		if (weightFile != null) {
			boolean includesAtLeastOneWeightForUppercaseChar = false;
			Map<Character, Map<Character, Float>> weightMap = new HashMap<Character, Map<Character, Float>>();
			weightMap = new HashMap<Character, Map<Character, Float>>();

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
					if (weightEntry[0].length() != 1 || weightEntry[1].length() != 1) {
						getContext().getLogger().log(Level.WARNING, "You provided a weight for '" + weightEntry[0]
								+ "' and '" + weightEntry[1]
								+ ", but only weights for single chars are accepted. This entry of the weight file will be ignored.");
						continue;
					}
					Character firstEntry = weightEntry[0].charAt(0);
					Character secondEntry = weightEntry[1].charAt(0);
					Float weight = 0.0f;
					try {
						weight = Float.parseFloat(weightEntry[2]);
					} catch (NumberFormatException e) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[2]
								+ "' for '" + weightEntry[0] + "' and '" + weightEntry[1]
								+ ", which cannot be parsed as a float. This entry of the weight file will be ignored.");
						continue;
					}
					if (weight < 1) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[2]
								+ "' for '" + weightEntry[0] + "' and '" + weightEntry[1]
								+ "'. In order for candidate generation to work properly please scale your distances to where the smallest distance is at least 1.0.");
					}
					if (!includesAtLeastOneWeightForUppercaseChar) {
						if (!Character.isLowerCase(firstEntry) || !Character.isLowerCase(secondEntry)) {
							includesAtLeastOneWeightForUppercaseChar = true;
						}
					}

					Map<Character, Float> currentCharacterMap = weightMap.get(firstEntry);
					if (currentCharacterMap == null) {
						weightMap.put(firstEntry, new HashMap<Character, Float>());
						currentCharacterMap = weightMap.get(firstEntry);
					}
					if (currentCharacterMap.get(secondEntry) == null) {
						currentCharacterMap.put(secondEntry, weight);
					} else {
						getContext().getLogger().log(Level.WARNING,
								"You provided two weights for '" + weightEntry[0] + "' to '" + weightEntry[1]
										+ "' (" + currentCharacterMap.get(weightEntry[1].toLowerCase().charAt(0))
										+ " and " + weightEntry[2] + ") in File" + weightFile
										+ ". The former will be used.");
					}
				}
				br.close();
				if (!lowercase && !includesAtLeastOneWeightForUppercaseChar) {
					getContext().getLogger().log(Level.INFO,
							"Parameter to process lowercased is disabled, but not a single weight including an uppercased char was provided. Uniform apitalization penalty will be applied if cases do not match.");
				}
				if (lowercase && includesAtLeastOneWeightForUppercaseChar) {
					getContext().getLogger().log(Level.INFO,
							"Parameter to process lowercased is enabled, but at least one weight including an uppercased char was provided. It will be ignored.");
				}
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
			boolean includesAtLeastOneWeightForUppercaseChar = false;
			Map<Character, Float> weightMap = new HashMap<Character, Float>();
			weightMap = new HashMap<Character, Float>();

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
					if (weightEntry[0].length() != 1) {
						getContext().getLogger().log(Level.WARNING, "You provided a weight for '" + weightEntry[0]
								+ ", but only weights for single chars are accepted. This entry of the weight file will be ignored.");
						continue;
					}
					Character character = weightEntry[0].charAt(0);
					Float weight = 0.0f;
					try {
						weight = Float.parseFloat(weightEntry[1]);
					} catch (NumberFormatException e) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[1]
								+ "' for '" + weightEntry[0]
								+ "', which cannot be parsed as a float. This entry of the weight file will be ignored.");
						continue;
					}
					if (weight < 1) {
						getContext().getLogger().log(Level.WARNING, "You provided the weight '" + weightEntry[1]
								+ "' for '" + weightEntry[0]
								+ "'. In order for candidate generation to work properly please scale your distances to where the smallest distance is at least 1.0.");
					}
					if (!includesAtLeastOneWeightForUppercaseChar) {
						if (!Character.isLowerCase(character)) {
							includesAtLeastOneWeightForUppercaseChar = true;
						}
					}

					Float currentWeight = weightMap.get(character);
					if (currentWeight == null) {
						weightMap.put(character, weight);
					} else {
						// Warn that there is double info for this char
						getContext().getLogger().log(Level.WARNING,
								"You provided two weights for '" + character + "' (" + currentWeight + " and "
										+ weight + ") in File" + weightFile + ". The former will be used.");
					}
				}
				br.close();
				if (!lowercase && !includesAtLeastOneWeightForUppercaseChar) {
					getContext().getLogger().log(Level.INFO,
							"Parameter to process lowercased is disabled, but not a single weight including an uppercased char was provided. Uniform apitalization penalty will be applied if cases do not match.");
				}
				if (lowercase && includesAtLeastOneWeightForUppercaseChar) {
					getContext().getLogger().log(Level.INFO,
							"Parameter to process lowercased is enabled, but at least one weight including an uppercased char was provided. It will be ignored.");
				}
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

		if (lowercase) {
			wrong = wrong.toLowerCase();
			right = right.toLowerCase();
		}

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
				// Strings were lowercased in beginning if lowercase == true
				if (wrong.charAt(wrongIndex - 1) == (right.charAt(rightIndex - 1))) {
					charsAreDifferent = 0;
				}

				float substitution = distanceMatrix[wrongIndex - 1][rightIndex - 1] + charsAreDifferent
						* getSubstitutionWeight(wrong.charAt(wrongIndex - 1), right.charAt(rightIndex - 1));

				// TRANSPOSITION
				if (includeTransposition && wrongIndex > 1 && rightIndex > 1
						&& wrong.charAt(wrongIndex - 1) == right.charAt(rightIndex - 2)
						&& wrong.charAt(wrongIndex - 2) == right.charAt(rightIndex - 1)) {
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

		if (insertionMap == null
//				|| !StringUtils.isAlpha(a + "")
		) {
			return defaultWeight;
		} else {
			try {
				if (lowercase) {
					a = Character.toLowerCase(a);
				}
				return insertionMap.get(a);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(Character.toString(a) + "ins")) {
					missingWeights.add(Character.toString(a) + "ins");
					getContext().getLogger().log(Level.INFO,
							"No insertion weight was provided for '" + a + "'. Applying default weight instead.");
				}
				return defaultWeight;
			}
		}
	}

	private float getDeletionWeight(char a) {
		if (deletionMap == null
//				|| !StringUtils.isAlpha(a + "")
		) {
			return defaultWeight;
		} else {
			try {
				if (lowercase) {
					a = Character.toLowerCase(a);
				}
				return deletionMap.get(a);
			} catch (NullPointerException e) {
				if (!missingWeights.contains(Character.toString(a) + "del")) {
					missingWeights.add(Character.toString(a) + "del");
					getContext().getLogger().log(Level.INFO,
							"No deletion weight was provided for '" + a + "'. Applying default weight instead.");
				}
				return defaultWeight;
			}
		}
	}

	private float getSubstitutionWeight(char a, char b) {

		if (a == b || (lowercase && lowercasedCharsAreEqual(a, b))) {
			return 0.0f;
		}
		if (substitutionMap == null
//				|| !StringUtils.isAlpha(a + "") || !StringUtils.isAlpha(b + "")
		) {
			// Did not provide weights, could still have said to punish upper
			float result = 0.0f;
			if (!lowercasedCharsAreEqual(a, b)) {
				result += defaultWeight;
			}
			if (!lowercase) {
				result += compareCases(a, b);
			}
			return result;
		} else {
			try {
				return substitutionMap.get(a).get(b);
			} catch (NullPointerException e) {
				String aString = Character.toString(a);
				String bString = Character.toString(b);
				if (!missingWeights.contains(aString + "sub") && !missingWeights.contains(aString + bString + "sub")) {
					if (substitutionMap.get(a) == null) {
						missingWeights.add(aString + "sub");
						getContext().getLogger().log(Level.INFO, "No substition weights were  provided for '" + a
								+ "'. Applying default weight (+ capitalization penalty if applicable) instead.");
					} else {
						missingWeights.add(aString + bString + "sub");
						getContext().getLogger().log(Level.INFO, "No substition weight was provided for '" + a
								+ "' and '" + b
								+ "'. Applying default weight (+ capitalization penalty if applicable) instead.");
					}
				}
				if (lowercase) {
					return defaultWeight;
				} else {
					return defaultWeight + compareCases(a, b);
				}
			}
		}
	}

	// Comparing whether cases are matching not necessary
	private float getTranspositionWeight(char a, char b) {
		if (a == b || (lowercase && lowercasedCharsAreEqual(a, b))) {
			return 0.0f;
		}
		if (transpositionMap == null
//				|| !StringUtils.isAlpha(a + "") || !StringUtils.isAlpha(b + "")
		) {
			return defaultWeight;
		} else {
			try {
				return transpositionMap.get(a).get(b);
			} catch (NullPointerException e) {
				String aString = Character.toString(a);
				String bString = Character.toString(b);
				if (!missingWeights.contains(aString + "tran")
						&& !missingWeights.contains(aString + bString + "tran")) {
					if (transpositionMap.get(a) == null) {
						missingWeights.add(aString + "tran");
						getContext().getLogger().log(Level.INFO, "No transposition weights were provided for '" + a
								+ "'. Applying default weight instead.");

					} else {
						missingWeights.add(aString + bString + "tran");
						getContext().getLogger().log(Level.INFO, "No transposition weight was provided for '" + a
								+ "' and '" + b + ". Applying default weight instead.");
					}
				}
				return defaultWeight;
			}
		}
	}

	private float compareCases(char a, char b) {

		if (Character.isLowerCase(a) == Character.isLowerCase(b)) {
			return 0.0f;
		} else {
			return capitalizationPenalty;
		}
	}

	private boolean lowercasedCharsAreEqual(char a, char b) {
		return Character.toLowerCase(a) == Character.toLowerCase(b);
	}

}