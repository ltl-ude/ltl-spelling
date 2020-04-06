package de.unidue.ltl.spelling.candidategeneration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import com.github.liblevenshtein.collection.dictionary.SortedDawg;
import com.github.liblevenshtein.transducer.Algorithm;
import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;
import com.github.liblevenshtein.transducer.factory.TransducerBuilder;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public abstract class CorrectionCandidateGenerator extends JCasAnnotator_ImplBase {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	protected String language;

	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	protected String[] dictionaries;

	public static final String PARAM_DISTANCE_THRESHOLD = "scoreThreshold";
	@ConfigurationParameter(name = PARAM_DISTANCE_THRESHOLD, mandatory = true, defaultValue = "1")
	protected int scoreThreshold;

	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	protected boolean includeTransposition;

	protected String defaultDictEN;
	protected String defaultDictDE;

	ITransducer<Candidate> transducer;
	SortedDawg dictionary;

	// Initialize resource to generate candidates (SortedDawg)
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		Set<String> dictionarySet = new HashSet<String>();

		initializeDefaultDictionary(dictionarySet);
		readAdditionalDictionaries(dictionarySet);

		List<String> dictionaryList = new ArrayList<String>();
		dictionaryList.addAll(dictionarySet);
		dictionaryList.sort(null);

		dictionary = new SortedDawg();
		dictionary.addAll(dictionaryList);

		Algorithm method;
		if (includeTransposition) {
			method = Algorithm.TRANSPOSITION;
		} else {
			method = Algorithm.STANDARD;
		}
		transducer = new TransducerBuilder().dictionary(dictionary).algorithm(method)
				.defaultMaxDistance(this.scoreThreshold).includeDistance(true).build();
	}

	protected abstract void initializeDefaultDictionary(Set<String> dictionarySet);

	protected abstract void readAdditionalDictionaries(Set<String> dictionarySet);

	// Create suggested actions for the generated candidates
	protected void addSuggestedActions(JCas aJCas, SpellingAnomaly anomaly, SuggestionCostTuples tuples) {
		if (tuples.size() > 0) {
			FSArray actions = new FSArray(aJCas, tuples.size());
			int i = 0;
			for (SuggestionCostTuple tuple : tuples) {
				SuggestedAction action = new SuggestedAction(aJCas);
				action.setReplacement(tuple.getSuggestion());
				action.setCertainty(tuple.getCertainty(tuples.getMaxCost()));
				actions.set(i, action);
				i++;
				System.out.println("Added new correction candidate: " + anomaly.getCoveredText() + "\t"
						+ action.getReplacement() + "\t" + action.getCertainty());
			}
			anomaly.setSuggestions(actions);
		} else {
			System.out.println("No correction found for: " + anomaly.getCoveredText());
		}
		System.out.println();
	}

	class SuggestionCostTuple {
		private final String suggestion;
		private final int cost;

		public SuggestionCostTuple(String suggestion, int cost) {
			this.suggestion = suggestion;
			this.cost = cost;
		}

		public String getSuggestion() {
			return suggestion;
		}

		public float getCertainty(int maxCost) {
			if (maxCost > 0) {
				return (float) maxCost / cost;
			} else {
				return 0f;
			}
		}
	}

	class SuggestionCostTuples implements Iterable<SuggestionCostTuple> {
		private final List<SuggestionCostTuple> tuples;
		private int maxCost;

		public SuggestionCostTuples() {
			tuples = new ArrayList<SuggestionCostTuple>();
			maxCost = 0;
		}

		public void addTuple(String suggestion, int cost) {
			tuples.add(new SuggestionCostTuple(suggestion, cost));

			if (cost > maxCost) {
				maxCost = cost;
			}
		}

		public int getMaxCost() {
			return maxCost;
		}

		public int size() {
			return tuples.size();
		}

//		@Override
		public Iterator<SuggestionCostTuple> iterator() {
			return tuples.iterator();
		}
	}
}
