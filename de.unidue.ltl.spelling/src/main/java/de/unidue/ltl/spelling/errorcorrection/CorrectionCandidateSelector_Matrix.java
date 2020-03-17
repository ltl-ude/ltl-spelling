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

public class CorrectionCandidateSelector_Matrix extends CorrectionCandidateSelector
{
	
	public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	@ExternalResource(key=PARAM_LANGUAGE_MODEL)
	private LanguageModelResource languageModel;
	
	public static final String PARAM_METHOD = "candidateSelectionMethod";
	@ConfigurationParameter(name = PARAM_METHOD, mandatory = false, defaultValue = "LEVENSHTEIN_UNIFORM")
	protected CandidateSelectionMethod candidateSelectionMethod;
	
	public enum CandidateSelectionMethod {
		LEVENSHTEIN_DISTANCE, KEYBOARD_DISTANCE, PHONETIC, LANGUAGE_MODEL_FREQUENCY, LANGUAGE_MODEL_PROBABILITY
	}
	
	Map<String, Integer> insert = new HashMap<String, Integer>();
	Map<String, Integer> delete = new HashMap<String, Integer>();
	//Outer map: char to replace; inner map: replacement, mapped to respective cost
	Map<Character, Map<Character,Integer>> substitute = new HashMap<Character, Map<Character, Integer>>();
	
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

    public String getBestSuggestion(SpellingAnomaly anomaly)
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
    
	public int calculateCosts (CharSequence lhs, CharSequence rhs) {                          
	    int len0 = lhs.length() + 1;                                                     
	    int len1 = rhs.length() + 1;                                                     
	                                                                                    
	    // the array of distances  
	    // cost: referring to s0
	    // newcost: referring to s1
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	                                                                                    
	    // initial cost of skipping prefix in String s0       
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	                                                                                    
	    // dynamically computing the array of distances                                  
	                                                                                    
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	                                                                                    
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;             
	                                                                                    
	            // computing cost for each transformation     
	            // if the chars do not match: look up substitution cost
	            int cost_replace = cost[i - 1] + match*substitute.get(lhs.charAt(i-1)).get(rhs.charAt(j-1));                                 
	            int cost_insert  = cost[i] + insert.get(rhs.charAt(j-1));                                         
	            int cost_delete  = newcost[i - 1] + delete.get(lhs.charAt(i-1));                                  
	                                                                                    
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	                                                                                    
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	                                                                                    
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];                                                          
	}
}
