package de.unidue.ltl.spelling.candidateselection;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class CorrectionCandidateSelector_CustomMatrixes extends CorrectionCandidateSelector {
	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	private boolean includeTransposition;

	public static final String PARAM_MAP_TRANSPOSITION = "tanspositionMapPath";
	@ConfigurationParameter(name = PARAM_MAP_TRANSPOSITION, mandatory = false)
	protected String transpositionMapPath;

	public static final String PARAM_MAP_SUBSTITUTION = "substitutionMapPath";
	@ConfigurationParameter(name = PARAM_MAP_SUBSTITUTION, mandatory = false)
	protected String substitutionMapPath;

	public static final String PARAM_MAP_INSERTION = "insertionMapPath";
	@ConfigurationParameter(name = PARAM_MAP_INSERTION, mandatory = false)
	protected String insertionMapPath;

	public static final String PARAM_MAP_DELETION = "deletionMapPath";
	@ConfigurationParameter(name = PARAM_MAP_DELETION, mandatory = false)
	protected String deletionMapPath;

	Map<String, Integer> insert = new HashMap<String, Integer>();
	Map<String, Integer> delete = new HashMap<String, Integer>();
	// Outer map: char to replace; inner map: replacement, mapped to respective cost
	Map<Character, Map<Character, Integer>> substitute = new HashMap<Character, Map<Character, Integer>>();
	Map<Character, Map<Character, Integer>> transpose = new HashMap<Character, Map<Character, Integer>>();
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		if (!includeTransposition && transpositionMapPath != null) {
			getContext().getLogger().log(Level.WARNING, "You provided a custom map for transposition costs as '"
					+ transpositionMapPath
					+ "' , but opted not to include transposition by setting PARAM_INCLUDE_TRANSPOSITION = false. Your custom transposition map will be ignored.");
		}
		
		// Aim is to minimize cost
		maximize = false;	
	}

	// TODO: Check method
	public int calculateCosts(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		// cost: referring to s0
		// newcost: referring to s1
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

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
				int cost_replace = cost[i - 1] + match * substitute.get(lhs.charAt(i - 1)).get(rhs.charAt(j - 1));
				int cost_insert = cost[i] + insert.get(rhs.charAt(j - 1));
				int cost_delete = newcost[i - 1] + delete.get(lhs.charAt(i - 1));

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction action) {
		return calculateCosts(anomaly.getCoveredText(), action.getReplacement());
	}
}
