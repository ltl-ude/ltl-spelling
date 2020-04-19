package de.unidue.ltl.spelling.candidateselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

//TODO: this may be able to share a common superclass with CustomMatrixes
public class CorrectionCandidateSelector_KeyboardDistance extends CorrectionCandidateSelector {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	private boolean includeTransposition;

	private final String keyboardDistanceMatrixGerman = "src/main/resources/matrixes/keyboardDistance_DE-manual.txt";
	private final String keyboardDistanceMatrixEnglish = "src/main/resources/matrixes/keyboardDistance_EN-manual.txt";

	private Map<Character, Map<Character, Double>> distanceMap = new HashMap<Character, Map<Character, Double>>();
	private final int defaultDistance = 4;
	private final int insertionCost = 4;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		// Initialize map from file
		String keyboardDistanceMatrix = null;
		if (language.contentEquals("de")) {
			keyboardDistanceMatrix = keyboardDistanceMatrixGerman;
		} else if (language.contentEquals("en")) {
			keyboardDistanceMatrix = keyboardDistanceMatrixEnglish;
		} else {
			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
			System.exit(1);
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(keyboardDistanceMatrix)));
			String line = null;
			String[] entries;
			Map<Character, Double> current;
			while (br.ready()) {
				line = br.readLine();
				entries = line.split("\t");
				current = distanceMap.get(entries[0].charAt(0));
				if (current == null) {
					distanceMap.put(entries[0].charAt(0), new HashMap<Character, Double>());
					current = distanceMap.get(entries[0].charAt(0));
				}
				current.put(entries[1].charAt(0), Double.parseDouble(entries[2]));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Aim is to minimize cost
		maximize = false;
	}

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
		return calculateCosts(anomaly.getCoveredText(), action.getReplacement());

	}

//	private double calculateCost(String misspelling, String correction) {
//
//		// TODO: handle upper/lower case differently?
//		misspelling = misspelling.toLowerCase();
//		correction = correction.toLowerCase();
//		Map<Character, Integer> da = new HashMap<Character, Integer>();
//		double[][] d = new double[misspelling.length() + 2][correction.length() + 2];
//
//		double maxdist = Integer.MAX_VALUE;
//		d[0][0] = maxdist;
//
//		// from 1 to end of array
//		for (int i = 0; i < misspelling.length() + 1; i++) {
//			d[i + 1][0] = maxdist;
//			d[i + 1][1] = i * 4;
//		}
//
//		// from 1 to end of array
//		for (int i = 0; i < correction.length() + 1; i++) {
//			d[0][i + 1] = maxdist;
//			d[1][i + 1] = i * 4;
//		}
//
//		for (int i = 1; i < misspelling.length() + 1; i++) {
//			int db = 0;
//			for (int j = 1; j < correction.length() + 1; j++) {
//				int k = 0;
//				try {
//					k = da.get(correction.charAt(j - 1));
//				} catch (Exception e) {
//
//				}
//				int l = db;
//				double cost = 1.0;
//				if (misspelling.charAt(i - 1) == correction.charAt(j - 1)) {
//					cost = 0.0;
//					db = j;
//				}
//
//				double substitution = d[i][j] + cost * getDistance(misspelling.charAt(i - 1), correction.charAt(j - 1));
//				// Estimate insertion cost as moderate distance
//				double insertion = d[i + 1][j] + defaultDistance;
//				// Min of distance to char before/after char to delete
//				double deletion = d[i][j + 1] + defaultDistance;
//				if (i > 1 && i < misspelling.length()) {
//					deletion = d[i][j + 1] + Math.min(getDistance(misspelling.charAt(i - 2), misspelling.charAt(i - 1)),
//							getDistance(misspelling.charAt(i - 1), misspelling.charAt(i)));
//				} else if (i > 1) {
//					deletion = d[i][j + 1] + getDistance(misspelling.charAt(i - 2), misspelling.charAt(i - 1));
//				} else if (i < misspelling.length()) {
//					deletion = d[i][j + 1] + getDistance(misspelling.charAt(i - 1), misspelling.charAt(i));
//				}
//				// TODO: transposition does not behave as expected when used with distance
//				// values
//				double transposition = Integer.MAX_VALUE;
//				if (includeTransposition && i > 1) {
//					transposition = d[k][l] + (i - k - 1)
//							+ getDistance(misspelling.charAt(i - 2), misspelling.charAt(i - 1)) + (j - l - 1);
//				}
//
//				d[i + 1][j + 1] = Math.min(Math.min(substitution, insertion), Math.min(deletion, transposition));
//			}
//			da.put(misspelling.charAt(i - 1), i);
//		}
//
//		for (int m = 0; m < misspelling.length() + 2; m++) {
//			for (int n = 0; n < correction.length() + 2; n++) {
//				System.out.print(d[m][n] + "\t");
//			}
//			System.out.println();
//		}
//
//		System.out.println("Distance between " + misspelling + " and " + correction + " is "
//				+ d[misspelling.length() + 1][correction.length() + 1]);
//		// TODO: must invert somehow because CorrectionCandidateSelector looks for
//		// highest value
//		return d[misspelling.length() + 1][correction.length() + 1];
//	}

	// Assumes performing operations on wrong to turn it into right
	private double calculateCosts(String wrong, String right) {

		// 0 when chars are the same, 1 otherwise
		int differentChars = -1;

		double deletion;
		double insertion;
		double substitution;
		double transposition;

		double[][] distanceMatrix = new double[wrong.length() + 1][right.length() + 1];

		for (int i = 0; i <= wrong.length(); i++) {
			// Cost of starting from scratch; if insertion cost depends on char it has to be
			// looked up
			distanceMatrix[i][0] = i * insertionCost;
//			distanceMatrix[i][0] = insert(wrong.charAt(i));
		}

		for (int j = 0; j <= right.length(); j++) {
			// Cost of starting from scratch; if insertion cost depends on char it has to be
			// looked up
			distanceMatrix[0][j] = j * insertionCost;
//			distanceMatrix[0][j] = insert(right.charAt(i));
		}

		for (int i = 1; i <= wrong.length(); i++) {
			for (int j = 1; j <= right.length(); j++) {

				differentChars = 1;

				if (wrong.charAt(i - 1) == (right.charAt(j - 1))) {
					differentChars = 0;
				}

				deletion = Double.MAX_VALUE;
				if (i > 1 && i < wrong.length() - 1) {
					deletion = distanceMatrix[i - 1][j]
							+ Math.min(getDistance(wrong.charAt(i - 2), wrong.charAt(i - 1)),
									getDistance(wrong.charAt(i - 1), wrong.charAt(i)));
				} else if (i > 1) {
					deletion = distanceMatrix[i - 1][j] + getDistance(wrong.charAt(i - 2), wrong.charAt(i - 1));
				} else if (i < wrong.length() - 1) {
					deletion = distanceMatrix[i - 1][j] + getDistance(wrong.charAt(i - 1), wrong.charAt(i));
				}

				insertion = distanceMatrix[i][j - 1] + insertionCost;

				substitution = distanceMatrix[i - 1][j - 1]
						+ differentChars * getDistance(wrong.charAt(i - 1), right.charAt(j - 1));

				if (includeTransposition && i > 1 && j > 1 && wrong.charAt(i - 1) == right.charAt(j - 2)
						&& wrong.charAt(i - 2) == right.charAt(j - 1)) {
					transposition = distanceMatrix[i - 2][j - 2]
							+ getDistance(wrong.charAt(i - 2), wrong.charAt(i - 1));
					distanceMatrix[i][j] = Math.min(deletion,
							Math.min(insertion, Math.min(substitution, transposition)));
				} else {
					distanceMatrix[i][j] = Math.min(deletion, Math.min(insertion, substitution));
				}

			}
		}

		for (int i = 0; i < wrong.length() + 1; i++) {
			for (int j = 0; j < right.length() + 1; j++) {

				System.out.print(distanceMatrix[i][j] + "\t");

			}
			System.out.println();
		}

		System.out.println("Distance between\t" + wrong + "\tand\t" + right + "\tis:\t"
				+ distanceMatrix[wrong.length()][right.length()]);
		return distanceMatrix[wrong.length()][right.length()];

	}

	private double getDistance(char a, char b) {
		try {
			// It is unlikely to accidentally repeat a character; therefore deleting an o
			// from hoouse should not cost 0 because the distance between o and o is 0
			if (a == b) {
				return defaultDistance + compareCases(a, b);
			}
			return distanceMap.get(Character.toLowerCase(a)).get(Character.toLowerCase(b)) + compareCases(a, b);
		} catch (NullPointerException e) {
//			System.out.println("Not found: " + a + b); 
			return defaultDistance + compareCases(a, b);
		}
	}

	private double compareCases(char a, char b) {

		if (Character.isLowerCase(a) == Character.isLowerCase(b)) {
			return 0.0;
		} else {
			return 0.5;
		}

	}
}