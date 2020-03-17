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

import java.util.List;

import org.apache.uima.fit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import eu.openminted.share.annotations.api.Component;
import eu.openminted.share.annotations.api.constants.OperationType;

/**
 * Converts annotations of the type SpellingAnomaly into SofaChangeAnnoatations.
 */
@Component(OperationType.NORMALIZER)

public class CorrectionCandidateSelector_Distance extends CorrectionCandidateSelector {

	public String getBestSuggestion(SpellingAnomaly anomaly) {
		Float bestCertainty = 0.0f;
		String bestReplacement = null;

		if (anomaly.getSuggestions() != null) {
			for (int i = 0; i < anomaly.getSuggestions().size(); i++) {
				Float currentCertainty = anomaly.getSuggestions(i).getCertainty();
				String currentReplacement = anomaly.getSuggestions(i).getReplacement();

				if (currentCertainty > bestCertainty) {
					bestCertainty = currentCertainty;
					bestReplacement = currentReplacement;
				}
			}
		}
		return bestReplacement;
	}

}
