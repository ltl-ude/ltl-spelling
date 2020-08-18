package de.unidue.ltl.spelling.generateAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

/**
 * Supertype for generate and rank methods
 */

public abstract class CandidateGeneratorAndRanker extends JCasAnnotator_ImplBase {

	/**
	 * The dictionaries based on which to generate the correction candidates.
	 */
	public static final String PARAM_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_DICTIONARIES, mandatory = true)
	protected String[] dictionaries;
	
	/**
	 * Number of candidates to be generated with this method. If there are more
	 * candidates with the same rank as the n-th of the top n candidates these are
	 * included as well.
	 */
	public static final String PARAM_NUM_OF_CANDIDATES_TO_GENERATE = "numberOfCandidatesToGenerate";
	@ConfigurationParameter(name = PARAM_NUM_OF_CANDIDATES_TO_GENERATE, mandatory = true, defaultValue = "5")
	protected int numberOfCandidatesToGenerate;
	
	protected Set<String> dictionary;

	protected void readDictionaries(String[] dictionaries) {
		dictionary = new HashSet<String>();
		for (String path : dictionaries) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(path)));
				while (br.ready()) {
					dictionary.add(br.readLine());
				}
				br.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public abstract void process(JCas aJCas) throws AnalysisEngineProcessException;
	
	protected SuggestionCostTuples getSuggestionCostTuples(Iterator<Entry<Float, List<String>>> entries) {
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
		
		return tuples;
	}

	// Create suggested actions for the generated candidates
	protected void addSuggestedActions(JCas aJCas, SpellingAnomaly anomaly, SuggestionCostTuples tuples) {

		if (tuples.size() > 0) {
			int i = 0;
			FSArray actions;

			// Copy whats there already
			if (anomaly.getSuggestions() != null) {
				int noOfAlreadyMadeSuggestions = anomaly.getSuggestions().size();
				actions = new FSArray(aJCas, noOfAlreadyMadeSuggestions + tuples.size());
				for (int s = 0; s < noOfAlreadyMadeSuggestions; s++) {
					actions.set(s, anomaly.getSuggestions(s));
				}
				i = noOfAlreadyMadeSuggestions;
			} else {
				actions = new FSArray(aJCas, tuples.size());
			}

			for (SuggestionCostTuple tuple : tuples) {
				SuggestedAction action = new SuggestedAction(aJCas);
				action.setReplacement(tuple.getSuggestion());
				action.setCertainty(tuple.getCost());
				actions.set(i, action);
				i++;
				System.out.println("Added new correction candidate:\t" + anomaly.getCoveredText() + "\t"
						+ action.getReplacement() + "\t" + action.getCertainty());
			}
			anomaly.setSuggestions(actions);
		} else {
			System.out.println("No correction found for:\t" + anomaly.getCoveredText());
		}
	}

	class SuggestionCostTuple {
		private final String suggestion;
		private final float cost;

		public SuggestionCostTuple(String suggestion, float cost) {
			this.suggestion = suggestion;
			this.cost = cost;
		}

		public String getSuggestion() {
			return suggestion;
		}

		public float getCost() {
			return cost;
		}
	}

	class SuggestionCostTuples implements Iterable<SuggestionCostTuple> {
		private final List<SuggestionCostTuple> tuples;

		public SuggestionCostTuples() {
			tuples = new ArrayList<SuggestionCostTuple>();
		}

		public void addTuple(String suggestion, float cost) {
			tuples.add(new SuggestionCostTuple(suggestion, cost));
		}

		public int size() {
			return tuples.size();
		}

		public Iterator<SuggestionCostTuple> iterator() {
			return tuples.iterator();
		}
	}
}