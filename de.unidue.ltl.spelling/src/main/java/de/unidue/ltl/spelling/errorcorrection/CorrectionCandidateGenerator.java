package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import com.github.liblevenshtein.collection.dictionary.SortedDawg;
import com.github.liblevenshtein.transducer.Algorithm;
import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;
import com.github.liblevenshtein.transducer.factory.TransducerBuilder;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.AnnotationChecker;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class CorrectionCandidateGenerator extends JCasAnnotator_ImplBase {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	protected String language;

	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	protected String[] dictionaries;

	public static final String PARAM_MODEL_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
	@ConfigurationParameter(name = PARAM_MODEL_ENCODING, mandatory = false, defaultValue = "UTF-8")
	protected String dictEncoding;

	public static final String PARAM_DISTANCE_THRESHOLD = "ScoreThreshold";
	@ConfigurationParameter(name = PARAM_DISTANCE_THRESHOLD, mandatory = true, defaultValue = "1")
	protected int scoreThreshold;

	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	protected boolean includeTransposition;

	protected String defaultDictEN;
	protected String defaultDictDE;

	ITransducer<Candidate>[] transducers;
	SortedDawg dictionary = null;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		transducers = new ITransducer[this.scoreThreshold];
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

		for (int i = 1; i <= this.scoreThreshold; i++) {
			ITransducer<Candidate> transducer = new TransducerBuilder().dictionary(dictionary).algorithm(method)
					.defaultMaxDistance(i).includeDistance(true).build();
			transducers[i - 1] = transducer;
		}
	}

	protected abstract void initializeDefaultDictionary(Set<String> dictionarySet);

	protected abstract void readAdditionalDictionaries(Set<String> dictionarySet);

	protected abstract String getTokenText(SpellingAnomaly spell);

	// To be called by process to determine candidates; can be implemented here
	public void generateCandidates(int maxDistance, Set<String> dictionary) {

	}

	class SuggestionCostTuple {
		private final String suggestion;
		private final Integer cost;

		public SuggestionCostTuple(String suggestion, Integer cost) {
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
