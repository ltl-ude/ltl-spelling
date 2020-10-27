package de.unidue.ltl.spelling.candidateReranking;

import java.io.IOException;
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
import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.resources.DummyFrequencyCountProvider;

public class LanguageModelReranker extends JCasAnnotator_ImplBase {

	public static final String RES_LANGUAGE_MODEL = "languageModel";
	@ExternalResource(key = RES_LANGUAGE_MODEL)
	private FrequencyCountProvider fcp;

	public static final String RES_LANGUAGE_MODEL_PROMPT_SPECIFIC = "promptSpecificLangugageModel";
	@ExternalResource(key = RES_LANGUAGE_MODEL_PROMPT_SPECIFIC)
	private FrequencyCountProvider fcp_specific;

	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true, defaultValue = "3")
	private int ngramSize;

	public static final String PARAM_SPECIFIC_LM_WEIGHT = "specificLMWeight";
	@ConfigurationParameter(name = PARAM_SPECIFIC_LM_WEIGHT, mandatory = false, defaultValue = "0.5f")
	private float specificLMweight;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		// Index to lookup the sentence an anomaly is contained in
		Map<SpellingAnomaly, Collection<Sentence>> index = JCasUtil.indexCovering(aJCas, SpellingAnomaly.class,
				Sentence.class);

		System.out.println("Number of sentences in text: " + JCasUtil.select(aJCas, Sentence.class).size());

		for (SpellingAnomaly anomaly : JCasUtil.select(aJCas, SpellingAnomaly.class)) {

			if (anomaly.getSuggestions() != null) {

				Collection<Sentence> sentences = index.get(anomaly);
				if (sentences.size() != 1) {
					System.err.println("SpellingAnomaly " + anomaly.getCoveredText()
							+ " is not covered by exactly one sentence in the text " + aJCas.getDocumentText());
					System.exit(0);
				}
				for (Sentence sentence : sentences) {
					List<Token> tokens = JCasUtil.selectCovered(aJCas, Token.class, sentence);
					FSArray suggestions = anomaly.getSuggestions();
					Map<SuggestedAction, Float> LMProbabilities = new HashMap<SuggestedAction, Float>();
					double maxProbability = 0;

					// Determine index of anomaly token within sentence
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

							double probabilityInDefaultLM = 1;
							double probabilityInCustomLM = 1;
							int start = minIndex;
							int end = start + ngramSizeToConsider - 1;

							while (end <= maxIndex) {
								String ngram = "";
								for (int i = start; i <= end; i++) {
									if (i == anomalyIndex) {
										// In case of a split token this will increase ngram size
										ngram = ngram + " " + candidate;
									} else {
										ngram = ngram + " " + tokens.get(i).getCoveredText();
									}
								}
								try {
//								System.out.println("Probability of " + ngram + " in main LM: " + fcp.getProbability(ngram));
									double defaultLMprob = fcp.getProbability(ngram);
									if (defaultLMprob > 0) {
										probabilityInDefaultLM = probabilityInDefaultLM * defaultLMprob;
										// Just as a precaution, fcp already smoothes
									} else {
										probabilityInDefaultLM = probabilityInDefaultLM
												* (1 / fcp.getNrOfNgrams(ngramSizeToConsider));
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
								if (!(fcp_specific instanceof DummyFrequencyCountProvider)) {
									try {
//									System.out.println("Probability of " + ngram + " in specific LM: " + fcp_specific.getProbability(ngram));
										double specificLMprob = fcp_specific.getProbability(ngram);
										if (specificLMprob > 0) {
											probabilityInCustomLM = probabilityInCustomLM * specificLMprob;
											// Just as a precaution, fcp already smoothes
										} else {
											probabilityInCustomLM = probabilityInCustomLM
													* (1 / fcp_specific.getNrOfNgrams(ngramSizeToConsider));
										}
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								start += 1;
								end += 1;
							}
							double probability = 1.0f;
							if (!(fcp_specific instanceof DummyFrequencyCountProvider)) {
								probability = probabilityInCustomLM * specificLMweight
										+ probabilityInDefaultLM * (1 - specificLMweight);
							} else {
								probability = probabilityInDefaultLM;
							}
							System.out.println("Putting probability for " + anomaly.getSuggestions(k).getReplacement()
									+ ": " + probability);
							LMProbabilities.put(anomaly.getSuggestions(k), (float) probability);
							if (probability > maxProbability) {
								maxProbability = probability;
							}
						}
					}

					for (SuggestedAction suggestedAction : LMProbabilities.keySet()) {
						System.out.println("Final probability of " + suggestedAction.getReplacement() + ": "
								+ LMProbabilities.get(suggestedAction) + " (normalized: "
								+ (float) maxProbability / LMProbabilities.get(suggestedAction) + ")");
						suggestedAction.setCertainty((float) maxProbability / LMProbabilities.get(suggestedAction));
					}
				}
			}
		}
	}
}