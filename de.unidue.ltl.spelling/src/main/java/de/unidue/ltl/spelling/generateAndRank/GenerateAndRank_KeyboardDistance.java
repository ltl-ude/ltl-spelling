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
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Generates and ranks candidates based on keyboard distance.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly" },
		// No real outputs, just SuggestedActions as entries to the SpellingAnomalies?
		outputs = { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction" })
public class GenerateAndRank_KeyboardDistance extends CandidateGeneratorAndRanker_LevenshteinBased {

	/**
	 * A file containing tab-separated line-by-line entries of distances between
	 * characters, e.g. "a\t b\t 5". Entries must be lowercase.
	 */
	public static final String PARAM_KEYBOARD_DISTANCES_FILE = "keyboardDistances";
	@ConfigurationParameter(name = PARAM_KEYBOARD_DISTANCES_FILE, mandatory = true)
	protected String keyboardDistancesPath;

	/**
	 * Sets the distance to apply when it is not reasonable to assume a presumed
	 * character modification was caused by keyboard layout. This distance is
	 * applied for all insertions needed to turn the supposed wrong word into one
	 * present in the dictionary, as well as for the distance of a character to
	 * itself and when no distance information was provided via the distance file.
	 * The default value corresponds to the average distance on a QWERTZ/Y keyboard.
	 */
	public static final String PARAM_DEFAULT_DISTANCE = "defaultDistance";
	@ConfigurationParameter(name = PARAM_DEFAULT_DISTANCE, mandatory = true, defaultValue = "3.6")
	protected float defaultDistance;

	/**
	 * Distance cost to add if cases do not match, e.g. 'A' to 's' is 1.0 + penalty.
	 */
	public static final String PARAM_CAPITALIZATION_PENALTY = "capitalizationPenalty";
	@ConfigurationParameter(name = PARAM_CAPITALIZATION_PENALTY, mandatory = true, defaultValue = "0.5")
	protected float capitalizationPenalty;

	private Map<Character, Map<Character, Float>> distanceMap = new HashMap<Character, Map<Character, Float>>();

	// To keep track and alert only once if a certain distance was not provided
	private Set<String> missingDistances = new HashSet<String>();

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionaries(dictionaries);
		readKeyboardDistances(keyboardDistancesPath);
	}

	private void readKeyboardDistances(String distanceFile) {
		boolean hasUpper = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(distanceFile)));
			while (br.ready()) {
				String line = br.readLine();
				String[] distanceEntry = line.split("\t");
				if (distanceEntry.length != 3) {
					getContext().getLogger().log(Level.WARNING,
							"Tab-separated triples of character, character and distance are expected, but file '"
									+ distanceFile + "' contained the line '" + line + ", which will be ignored.");
					continue;
				}
				if ((distanceEntry[0].length() != 1) || (distanceEntry[1].length() != 1)) {
					getContext().getLogger().log(Level.WARNING, "You provided a distance for '" + distanceEntry[0]
							+ "' to '" + distanceEntry[1]
							+ "', but only weights for single chars are accepted. This entry of the distance file will be ignored.");
					continue;
				}
				Character from = distanceEntry[0].charAt(0);
				Character to = distanceEntry[1].charAt(0);
				Float distance = 0.0f;
				try {
					distance = Float.parseFloat(distanceEntry[2]);
				} catch (NumberFormatException e) {
					getContext().getLogger().log(Level.WARNING, "You provided the distance '" + distanceEntry[2]
							+ "' for '" + distanceEntry[0] + "' and '" + distanceEntry[1]
							+ "', which cannot be parsed as a float. This entry of the distance file will be ignored.");
					continue;
				}
				if (distance < 1) {
					getContext().getLogger().log(Level.WARNING, "You provided the distance '" + distanceEntry[2]
							+ "' for '" + distanceEntry[0] + "' and '" + distanceEntry[1]
							+ "'. In order for candidate generation to work properly please scale your distances to where the smallest distance is at least 1.0.");
				}
				if (!hasUpper) {
					if (Character.isUpperCase(from) || Character.isUpperCase(to)) {
						hasUpper = true;
					}
				}

				Map<Character, Float> currentCharacterMap = distanceMap.get(from);
				if (currentCharacterMap == null) {
					distanceMap.put(from, new HashMap<Character, Float>());
					currentCharacterMap = distanceMap.get(from);
				}
				addDistance(from, to, distance, currentCharacterMap);

				// Also put in the other way round, because distances are assumed to be
				// symmetric
				currentCharacterMap = distanceMap.get(to);
				if (currentCharacterMap == null) {
					distanceMap.put(to, new HashMap<Character, Float>());
					currentCharacterMap = distanceMap.get(to);
				}
				addDistance(to, from, distance, currentCharacterMap);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (hasUpper) {
			getContext().getLogger().log(Level.INFO,
					"You provided two at least one distance for an uppercased character. These distances will never be used.");
		}
	}

	private void addDistance(char from, char to, float distance, Map<Character, Float> map) {
		if (map.get(to) == null || map.get(to) == distance) {
			map.put(to, distance);
		} else {
			getContext().getLogger().log(Level.WARNING,
					"You provided two different distances for '" + from + "' and '" + to + "' (" + map.get(to) + " and "
							+ distance + "), but distances are assumed to be symmetric. The former will be used.");
		}
	}

	// Assumes performing operations on 'wrong' to turn it into 'right'
	@Override
	protected float calculateCost(String wrong, String right) {

		float[][] distanceMatrix = new float[wrong.length() + 1][right.length() + 1];

		// Worst case: cost of starting from scratch
		for (int i = 0; i <= wrong.length(); i++) {
			distanceMatrix[i][0] = i * defaultDistance;
		}
		for (int j = 0; j <= right.length(); j++) {
			distanceMatrix[0][j] = j * defaultDistance;
		}

		for (int wrongIndex = 1; wrongIndex <= wrong.length(); wrongIndex++) {
			for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {

				// INSERTION: Apply default cost, as no inference can be made about the
				// keyboard layout causing people to forget a certain char.
				float insertion = distanceMatrix[wrongIndex][rightIndex - 1] + defaultDistance;

				// DELETION: If b is to be deleted from abc, cost = min(delete b from ab, delete
				// b from bc)
				float compareToCharBefore = Float.MAX_VALUE;
				float compareToCharAfter = Float.MAX_VALUE;
				if (wrongIndex > 1) {
					compareToCharBefore = getDistance(wrong.charAt(wrongIndex - 2), wrong.charAt(wrongIndex - 1));
				}
				if (wrongIndex < wrong.length()) {
					compareToCharAfter = getDistance(wrong.charAt(wrongIndex - 1), wrong.charAt(wrongIndex));
				}
				float deletion = distanceMatrix[wrongIndex - 1][rightIndex]
						+ Math.min(compareToCharBefore, compareToCharAfter);

				// SUBSTITUTION
				int charsAreDifferent = 1;
				if (wrong.charAt(wrongIndex - 1) == (right.charAt(rightIndex - 1))) {
					charsAreDifferent = 0;
				}
				float substitution = distanceMatrix[wrongIndex - 1][rightIndex - 1]
						+ charsAreDifferent * getDistance(wrong.charAt(wrongIndex - 1), right.charAt(rightIndex - 1));

				// TRANSPOSITION
				if (includeTransposition && wrongIndex > 1 && rightIndex > 1
						&& wrong.charAt(wrongIndex - 1) == right.charAt(rightIndex - 2)
						&& wrong.charAt(wrongIndex - 2) == right.charAt(rightIndex - 1)) {
					float transposition = distanceMatrix[wrongIndex - 2][rightIndex - 2]
							+ getDistance(wrong.charAt(wrongIndex - 2), wrong.charAt(wrongIndex - 1));

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

	private float getDistance(char a, char b) {

		try {
			// It is unlikely to accidentally repeat a character; therefore deleting an o
			// from hoouse should not cost 0 because the distance between o and o is 0;
			// apply default distance instead
			if (lowercasedCharsAreEqual(a, b)) {
				return defaultDistance + compareCases(a, b);
			}
			return distanceMap.get(Character.toLowerCase(a)).get(Character.toLowerCase(b)) + compareCases(a, b);
		} catch (NullPointerException e) {
			String aString = Character.toString(a);
			String bString = Character.toString(b);
			if (!missingDistances.contains(aString) && !missingDistances.contains(bString)
					&& !missingDistances.contains(aString + bString)) {
				if (distanceMap.get(a) == null) {
					missingDistances.add(aString);
					getContext().getLogger().log(Level.INFO,
							"No distances were provided for '" + a + "'. Applying default distance instead.");
				} else if (distanceMap.get(b) == null) {
					missingDistances.add(bString);
					getContext().getLogger().log(Level.INFO,
							"No distances were provided for '" + b + "'. Applying default distance instead.");
				} else {
					missingDistances.add(aString + bString);
					getContext().getLogger().log(Level.INFO, "No distance was provided for '" + a + "' and '" + b
							+ "'. Applying default distance instead.");
				}
			}
			return defaultDistance + compareCases(a, b);
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
		return ((a + "").toLowerCase().equals((b + "").toLowerCase()));
	}
}