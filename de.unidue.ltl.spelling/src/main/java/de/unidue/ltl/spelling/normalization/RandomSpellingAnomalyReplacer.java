package de.unidue.ltl.spelling.normalization;

import java.util.Collections;
import java.util.List;

/**
 * In case of multiple candidates with an equally well score, pick one at random
 */

public class RandomSpellingAnomalyReplacer extends SpellingAnomalyReplacer {	
	
	@Override
	protected String getBestReplacement(List<String> replacements) {
		Collections.shuffle(replacements);
		return replacements.get(0);
	}
}
