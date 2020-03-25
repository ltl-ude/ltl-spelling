package de.unidue.ltl.spelling.errorcorrection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	
	//This matrix is used threefold:
	//Deletion: min{distance between char_to_delete and previous char, distance between char_to_delete and subsequent char}
	//Substition
	//Transposition
	private final String keyboardDistanceMatrixGerman = "src/main/resources/matrixes/keyboardDistance_DE.txt";
	private final String keyboardDistanceMatrixEnglish = "src/main/resources/matrixes/keyboardDistance_EN.txt";
	//Insertion cost is constantly 1
	
	private Map<String,double[]> distanceMap;
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		//Initialize map from file
		String keyboardDistanceMatrix = null;
		if(language.contentEquals("de")) {
			keyboardDistanceMatrix = keyboardDistanceMatrixGerman;
		}
		else if(language.contentEquals("en")) {
			keyboardDistanceMatrix = keyboardDistanceMatrixEnglish;
		}
		else {
			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
			System.exit(1);
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(keyboardDistanceMatrix)));
			String line;
			String[] entries;
			while(br.ready()) {
				line = br.readLine();
				entries = line.split("\t");
				distanceMap.put(entries[0],new double[]{Double.parseDouble(entries[1]),Double.parseDouble(entries[2])});
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
		return calculateCost(anomaly.getCoveredText(), action.getReplacement());
	}
	
	// TODO: Check method
	public double calculateCost(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		// cost: referring to s0
		// newcost: referring to s1
		double[] cost = new double[len0];
		double[] newcost = new double[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++)
			cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for (int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				// if the chars do not match: look up substitution cost
				double cost_replace = cost[i - 1] + match * getDistance(lhs.charAt(i - 1),rhs.charAt(j - 1));
				double cost_insert = cost[i] + 1;
				double cost_delete = newcost[i - 1] + Math.min(getDistance(lhs.charAt(i-2),lhs.charAt(i-1)), getDistance(lhs.charAt(i-1),lhs.charAt(i)));

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			double[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}
	
	private double getDistance(char a, char b) {
		double result = Math.abs(distanceMap.get(a)[0] - distanceMap.get(b)[0]);
		result += Math.abs(distanceMap.get(a)[1] - distanceMap.get(b)[1]);
		return result;
	}
	
}
