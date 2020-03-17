/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.dkpro.core.api.transform.JCasTransformerChangeBased_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.resources.LanguageModelResource;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import eu.openminted.share.annotations.api.Component;
import eu.openminted.share.annotations.api.DocumentationResource;
import eu.openminted.share.annotations.api.constants.OperationType;

/**
 * Converts annotations of the type SpellingAnomaly into SofaChangeAnnoatations.
 */
@Component(OperationType.NORMALIZER)

public class CorrectionCandidateSelector_LanguageModel extends CorrectionCandidateSelector {

	public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	@ExternalResource(key = PARAM_LANGUAGE_MODEL)
	private LanguageModelResource languageModel;

	public static final String PARAM_SENTENCE_LEVEL = "sentenceLevel";
	@ConfigurationParameter(name = PARAM_SENTENCE_LEVEL, mandatory = true, defaultValue = "false")
	protected boolean sentenceLevel;

	public String getBestSuggestion(SpellingAnomaly anomaly) {
		if (sentenceLevel) {
			return getBestSuggestion_Sentence(anomaly);
		} else {
			return getBestSuggestion_Token(anomaly);
		}
	}

	//TODO: set ngram size?
	private String getBestSuggestion_Token(SpellingAnomaly anomaly) {
		Float highestFrequency = 0.0f;
		String bestReplacement = null;
		String currentReplacement = null;

		if (anomaly.getSuggestions() != null) {
			for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
				currentReplacement = anomaly.getSuggestions(i).getReplacement();
				Float currentFrequency = languageModel.getFrequency(currentReplacement);
				if (currentFrequency > highestFrequency) {
					highestFrequency = currentFrequency;
					bestReplacement = currentReplacement;
				}
			}
		}
		return bestReplacement;
	}

	//TODO: set ngram size
	//TODO: needs the whole sentence, not just the anomaly
	private String getBestSuggestion_Sentence(SpellingAnomaly anomaly) {
		Float highestFrequency = 0.0f;
		String bestReplacement = null;
		String currentReplacement = null;

		if (anomaly.getSuggestions() != null) {
			for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
				currentReplacement = anomaly.getSuggestions(i).getReplacement();
				Float currentFrequency = languageModel.getFrequency(currentReplacement);
				if (currentFrequency > highestFrequency) {
					highestFrequency = currentFrequency;
					bestReplacement = currentReplacement;
				}
			}
		}
		return bestReplacement;
	}

}
