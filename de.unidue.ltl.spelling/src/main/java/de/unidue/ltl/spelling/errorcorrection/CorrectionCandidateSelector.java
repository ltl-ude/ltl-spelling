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

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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

public class CorrectionCandidateSelector extends JCasTransformerChangeBased_ImplBase
{
	
	public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	@ExternalResource(key=PARAM_LANGUAGE_MODEL)
	private LanguageModelResource languageModel;
	
    @Override
    public void process(JCas aInput, JCas aOutput) throws AnalysisEngineProcessException
    {
    		System.out.println(select(aInput,SpellingAnomaly.class).size());
	        for (SpellingAnomaly anomaly : select(aInput, SpellingAnomaly.class)) {
	            replace(anomaly.getBegin(), anomaly.getEnd(), getBestSuggestion(anomaly));
//	            SpellingAnomalyTransfer exAn = new SpellingAnomalyTransfer(aOutput);
//	            exAn.setBegin(anomaly.getBegin());
//	            exAn.setEnd(anomaly.getEnd());
////	            exAn.setSuggestions(anomaly.getSuggestions());
//	            exAn.addToIndexes();
	        }
    }

    private String getBestSuggestion(SpellingAnomaly anomaly)
    {
        Float bestCertainty = 0.0f;
        List<Token> tokens = JCasUtil.selectCovered(Token.class, anomaly);
        if(tokens.size() > 1) {
        	System.out.println("Found more than one token annotation for same string, using the first one.");
        }
        String bestReplacement = tokens.get(0).getCoveredText();

        if(anomaly.getSuggestions() != null) {
	        for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
	            Float currentCertainty = anomaly.getSuggestions(i).getCertainty();
	            String currentReplacement = anomaly.getSuggestions(i).getReplacement();
//		        System.out.println("Frequency in LM: "+currentReplacement+"\t"+languageModel.getFrequency(currentReplacement));
	
	            if (currentCertainty > bestCertainty) {
	                bestCertainty = currentCertainty;
	                bestReplacement = currentReplacement;
	            }
	        }
        }
        return bestReplacement;
    }
}
