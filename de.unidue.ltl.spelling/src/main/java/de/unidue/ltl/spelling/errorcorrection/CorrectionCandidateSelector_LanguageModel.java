package de.unidue.ltl.spelling.errorcorrection;

import java.util.List;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class CorrectionCandidateSelector_LanguageModel extends CorrectionCandidateSelector {

	public static final String PARAM_DEFAULT_LANGUAGE_MODEL = "defaultLanguageModel";
	@ExternalResource(key = PARAM_DEFAULT_LANGUAGE_MODEL)
	private LanguageModelResource defaultLanguageModel;
	
	public static final String PARAM_CUSTOM_LANGUAGE_MODEL = "customLanguageModel";
	@ExternalResource(key = PARAM_CUSTOM_LANGUAGE_MODEL)
	private LanguageModelResource customLanguageModel;
	
	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true, defaultValue = "2")
	private int ngramSize;
	
	public static final String PARAM_CUSTOM_LM_WEIGHT = "customLMWeight";
	@ConfigurationParameter(name = PARAM_CUSTOM_LM_WEIGHT, mandatory = false)
	private double customLMWeight;

	@Override
	protected double getValue(JCas aJCas, SpellingAnomaly anomaly, SuggestedAction currentSuggestion) {
		double probabilityInDefaultLM = 1.0;
		double probabilityInCustomLM = 1.0;
//		JCasUtil.indexCovering(aJCas, currentSuggestion, Sentence.class);
		List<Sentence> sentences = JCasUtil.selectCovering(Sentence.class, anomaly);
		// Should only be one
		for(Sentence sentence : sentences) {
//			System.out.println(sentence.getCoveredText());
			List<Token> tokens = JCasUtil.selectCovered(aJCas, Token.class, sentence);
			//Determine token of anomaly, include n-1 tokens before and after that to determine probability
			int anomalyIndex = -1;
			for(int i=0; i<tokens.size(); i++) {
				Token t = tokens.get(i);
				if(t.getBegin() == anomaly.getBegin() && t.getEnd() == anomaly.getEnd()) {
					anomalyIndex = i;
					break;
				}
			}
//			System.out.println("anomaly index: "+anomalyIndex);
			int minIndex = -1;
			int maxIndex = -1;
			//Should never happen
			if(anomalyIndex == -1) {
				System.out.println("Could not find anomaly "+anomaly.getCoveredText()+" in sentence "+sentence.getCoveredText());
				System.exit(0);
			}
			else{
				minIndex = anomalyIndex - ngramSize +1;
				//Can at most be beginning of the sentence, but not negative
				if(minIndex < 0) {
					minIndex = 0;
				}
				maxIndex = anomalyIndex + ngramSize -1;
				//Prevent index from being out of sentence range
				if(maxIndex >= tokens.size()) {
					maxIndex = tokens.size()-1;
				}
//				System.out.println("min index: "+minIndex);
//				System.out.println("max index: "+maxIndex);
				int ngramSizeToConsider;
				//Check if an at least one ngram of the desired size fits into the determined span
				if(maxIndex - minIndex < ngramSize -1) {
					//Must consider smaller ngram size for this sentence
					ngramSizeToConsider = maxIndex - minIndex + 1;
				}
				else {
					ngramSizeToConsider = ngramSize;
				}
//				System.out.println("Ngram size to consider: "+ngramSizeToConsider);
				//Get probability of ngrams from lm, multiply
				int start = minIndex;
				int end = start + ngramSizeToConsider - 1;
				while(end<=maxIndex) {
//					System.out.println("start: "+start);
//					System.out.println("end: "+end);
					String[] ngram = new String[ngramSizeToConsider];
					int j = 0;
					for(int i = start; i<= end; i++) {
						ngram[j] = tokens.get(i).getCoveredText();
						j++;
					}
					probabilityInDefaultLM = probabilityInDefaultLM * defaultLanguageModel.getFrequency(ngram);
					probabilityInCustomLM = probabilityInCustomLM * customLanguageModel.getFrequency(ngram);
					start += 1;
					end += 1;
				}
			}
			
		}
		double probability = 1.0;
		if(probabilityInCustomLM != 0) {
			probability = probabilityInCustomLM * customLMWeight + probabilityInDefaultLM * (1-customLMWeight);
		}
		else {
			probability = probabilityInDefaultLM;
		}
		return probability;
	}
}
