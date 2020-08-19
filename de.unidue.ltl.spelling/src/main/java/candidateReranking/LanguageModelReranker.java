package candidateReranking;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class LanguageModelReranker extends JCasAnnotator_ImplBase {

	public static final String PARAM_DEFAULT_LANGUAGE_MODEL = "defaultLanguageModel";
	@ExternalResource(key = PARAM_DEFAULT_LANGUAGE_MODEL)
	private LanguageModelResource defaultLanguageModel;

	public static final String PARAM_CUSTOM_LANGUAGE_MODEL = "customLanguageModel";
	@ExternalResource(key = PARAM_CUSTOM_LANGUAGE_MODEL)
	private LanguageModelResource customLanguageModel;

	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true, defaultValue = "1")
	private int ngramSize;

	public static final String PARAM_CUSTOM_LM_WEIGHT = "customLMWeight";
	@ConfigurationParameter(name = PARAM_CUSTOM_LM_WEIGHT, mandatory = false, defaultValue = "0.5f")
	private float customLMWeight;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Map<SpellingAnomaly, Collection<Sentence>> index = JCasUtil.indexCovering(aJCas, SpellingAnomaly.class,
				Sentence.class);

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {

			Collection<Sentence> sentences = index.get(anomaly);
			if (sentences.size() > 1) {
				System.err.println("SpellingAnomaly " + anomaly.getCoveredText()
						+ " is covered by more than one sentence in text " + aJCas.getDocumentText());
				System.exit(0);
			}
			for (Sentence sentence : sentences) {
				List<Token> tokens = JCasUtil.selectCovered(aJCas, Token.class, sentence);
				FSArray suggestions = anomaly.getSuggestions();
				Map<SuggestedAction, Float> LMProbabilities = new HashMap<SuggestedAction, Float>();
				float maxProbability = 0;
				
				// Determine index of anomaly token
				// Include n-1 tokens before and after that to determine probability
				int anomalyIndex = -1;
				for (int i = 0; i < tokens.size(); i++) {
					Token t = tokens.get(i);
					if (t.getBegin() == anomaly.getBegin() && t.getEnd() == anomaly.getEnd()) {
						anomalyIndex = i;
						break;
					}
				}
				int minIndex = -1;
				int maxIndex = -1;

				// Should never happen
				if (anomalyIndex == -1) {
					System.out.println("Could not find anomaly " + anomaly.getCoveredText() + " in sentence "
							+ sentence.getCoveredText());
					System.exit(0);
				} else {
					minIndex = anomalyIndex - ngramSize + 1;
					// Can at most be beginning of the sentence, but not negative
					if (minIndex < 0) {
						minIndex = 0;
					}
					maxIndex = anomalyIndex + ngramSize - 1;
					// Prevent index from being out of sentence range
					if (maxIndex >= tokens.size()) {
						maxIndex = tokens.size() - 1;
					}
					int ngramSizeToConsider;
					// Does at least one ngram of the desired size fit into the determined span?
					if (maxIndex - minIndex < ngramSize - 1) {
						// Must consider smaller ngram size for this sentence
						ngramSizeToConsider = maxIndex - minIndex + 1;
					} else {
						ngramSizeToConsider = ngramSize;
					}

					for (int k = 0; k < suggestions.size(); k++) {
						String candidate = anomaly.getSuggestions(k).getReplacement();
						float probabilityInDefaultLM = 1;
						float probabilityInCustomLM = 1;
						// Get probability of ngrams from lm, multiply
						int start = minIndex;
						int end = start + ngramSizeToConsider - 1;
						while (end <= maxIndex) {
							String[] ngram = new String[ngramSizeToConsider];
							int j = 0;
							for (int i = start; i <= end; i++) {
								if (i == anomalyIndex) {
									// TODO: in case of a split token this will increase ngram size
									ngram[j] = candidate;
								} else {
									ngram[j] = tokens.get(i).getCoveredText();
								}
								j++;
							}
//							probabilityInDefaultLM = probabilityInDefaultLM * defaultLanguageModel.getFrequency(ngram);
//							if (customLanguageModel != null) {
//								probabilityInCustomLM = probabilityInCustomLM * customLanguageModel.getFrequency(ngram);
//							}
							start += 1;
							end += 1;
						}
						Float probability = 1.0f;
						if (customLanguageModel != null) {
							probability = probabilityInCustomLM * customLMWeight + probabilityInDefaultLM * (1 - customLMWeight);
						} else {
							probability = probabilityInDefaultLM;
						}
						LMProbabilities.put(anomaly.getSuggestions(k), probability);
						if(probability > maxProbability) {
							maxProbability = probability;
						}
					}
				}

				for(SuggestedAction suggestedAction : LMProbabilities.keySet()) {
					suggestedAction.setCertainty(maxProbability/LMProbabilities.get(suggestedAction));
				}

			}
		}
	}
}