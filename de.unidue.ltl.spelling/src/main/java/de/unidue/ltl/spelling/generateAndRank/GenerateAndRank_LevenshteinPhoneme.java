package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.unidue.ltl.spelling.generateAndRank.CandidateGeneratorAndRanker.SuggestionCostTuples;
import de.unidue.ltl.spelling.utils.PhonemeUtils;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Generates and ranks candidates based on (weighted) levenshtein distance and
 * phonemes.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly" },
		// No real outputs, just SuggestedActions as entries to the SpellingAnomalies?
		outputs = { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction" })
public class GenerateAndRank_LevenshteinPhoneme extends CandidateGeneratorAndRanker {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	protected String language;

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

	/**
	 * Whether to permit transposition as a modification operation, e.g. apply
	 * Damerau-Levenshtein distance as opposed to standard Levenshtein Distance.
	 */
	public static final String PARAM_GRAPHEME_TO_PHONEME_FILE = "graphemeToPhonemeMapFile";
	@ConfigurationParameter(name = PARAM_GRAPHEME_TO_PHONEME_FILE, mandatory = true)
	protected String graphemeToPhonemeMapFile;

	private Map<Character, Float> deletionMap;
	private Map<Character, Float> insertionMap;
	private Map<Character, Map<Character, Float>> substitutionMap;
	private Map<Character, Map<Character, Float>> transpositionMap;

	private Map<String, String> graphemeToPhonemeMap;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		graphemeToPhonemeMap = readG2PMap(graphemeToPhonemeMapFile);
		dictionary = graphemeToPhonemeMap.keySet();

		deletionMap = readWeights(weightFileDeletion);
		insertionMap = readWeights(weightFileInsertion);
		substitutionMap = readWeights2D(weightFileSubstitution);
		if (includeTransposition && weightFileTransposition != null) {
			getContext().getLogger().log(Level.WARNING,
					"Transposition was not chosen to be included in GenerateAndRank_LevenshteinGrapheme, but you provided the file'"
							+ weightFileTransposition + "' with transposition weights. They will not be included.");
		} else {
			transpositionMap = readWeights2D(weightFileTransposition);
		}

		System.out.println(deletionMap);
		System.out.println(insertionMap);
		System.out.println(substitutionMap);
		System.out.println(transpositionMap);
	}

	private Map<String, String> readG2PMap(String mapFile) {
		Map<String, String> g2pMap = new HashMap<String, String>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(mapFile)));
			while (br.ready()) {
				String line = br.readLine();
				String[] entry = line.split(";");
				g2pMap.put(entry[0], entry[1]);
			}
			br.close();
			return g2pMap;
		} catch (IOException e) {
			getContext().getLogger().log(Level.WARNING, "Error reading g2p map.");
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}

	private Map<Character, Map<Character, Float>> readWeights2D(String weightFile) {
		if (weightFile != null) {
			Map<Character, Map<Character, Float>> weightMap = new HashMap<Character, Map<Character, Float>>();

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					Map<Character, Float> currentCharacterMap = weightMap.get(weightEntry[0].charAt(0));
					if (currentCharacterMap == null) {
						weightMap.put(weightEntry[0].charAt(0), new HashMap<Character, Float>());
						currentCharacterMap = weightMap.get(weightEntry[0].charAt(0));
					}
					if (currentCharacterMap.get(weightEntry[1].charAt(0)) == null) {
						currentCharacterMap.put(weightEntry[1].charAt(0), Float.parseFloat(weightEntry[2]));
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

			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(weightFile)));
				while (br.ready()) {
					String line = br.readLine();
					String[] weightEntry = line.split("\t");
					Float currentWeight = weightMap.get(weightEntry[0].charAt(0));
					if (currentWeight == null) {
						weightMap.put(weightEntry[0].charAt(0), Float.parseFloat(weightEntry[1]));
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
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {
			System.out.println();
			String misspelling = anomaly.getCoveredText();
			misspelling = PhonemeUtils.getPhoneme(misspelling, language);
			Map<Float, List<String>> rankedCandidates = new TreeMap<Float, List<String>>();

			for (String word : dictionary) {
				String wordPhonemes = graphemeToPhonemeMap.get(word);
				float cost = calculateCost(misspelling, wordPhonemes);
				List<String> rankList = rankedCandidates.get(cost);
				if (rankList == null) {
					rankedCandidates.put(cost, new ArrayList<String>());
					rankList = rankedCandidates.get(cost);
				}
				rankList.add(word);
			}

			Iterator<Entry<Float, List<String>>> entries = rankedCandidates.entrySet().iterator();
			SuggestionCostTuples tuples = new SuggestionCostTuples();

			while (tuples.size() < numberOfCandidatesToGenerate) {
				if (entries.hasNext()) {
					Entry<Float, List<String>> entry = entries.next();
					List<String> currentRankList = entry.getValue();
					float rank = entry.getKey();
					for (int j = 0; j < currentRankList.size(); j++) {
						tuples.addTuple(currentRankList.get(j), rank);
					}
				} else {
					break;
				}
			}

			addSuggestedActions(aJCas, anomaly, tuples);
		}
		System.out.println();
	}

	// Assumes performing operations on 'wrong' to turn it into 'right'
	// Called with phonetic transcriptions
	@Override
	protected float calculateCost(String wrong, String right) {

		wrong = wrong.replaceAll(" \\. ", " ");
		right = right.replaceAll(" \\. ", " ");
		
		wrong = wrong.replaceAll(" ' ", " ");
		right = right.replaceAll(" ' ", " ");
		
//		wrong = wrong.replaceAll(" ", "");
//		right = right.replaceAll(" ", "");

//		System.out.println("Calculating cost for "+wrong+" and "+right);
		
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
						&& (wrong.charAt(wrongIndex - 1) == right.charAt(rightIndex - 2))
						&& (wrong.charAt(wrongIndex - 2) == right.charAt(rightIndex - 1))) {
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
				System.out.println("No insertion weight found for char " + a + ". Applying default weight instead.");
				return defaultWeight;
			}
		}
	}

	private float getDeletionWeight(char a) {
		if (deletionMap == null) {
			return defaultWeight;
		} else {
			try {
				return deletionMap.get(a);
			} catch (NullPointerException e) {
				System.out.println("No deletion weight found for char " + a + ". Applying default weight instead.");
				return defaultWeight;
			}
		}
	}

	private float getSubstitutionWeight(char a, char b) {
		if ((a == b)) {
			return 0;
		}
		if (substitutionMap == null) {
			return defaultWeight;
		} else {
			try {
				return substitutionMap.get(a).get(b);
			} catch (NullPointerException e) {
				System.out.println("No substition weight found for chars " + a + " and " + b
						+ ". Applying default weight instead.");
				return defaultWeight;
			}
		}
	}

	private float getTranspositionWeight(char a, char b) {
		if (transpositionMap == null) {
			return defaultWeight;
		} else {
			try {
				return transpositionMap.get(a).get(b);
			} catch (NullPointerException e) {
				System.out.println("No transposition weight found for chars " + a + " and " + b
						+ ". Applying default weight instead.");
				return defaultWeight;
			}
		}
	}
}