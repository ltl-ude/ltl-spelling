package de.unidue.ltl.spelling.normalization;

import java.util.Collections;
import java.util.List;

/**
 * In case of multiple candidates with an equally well score, pick one at random
 */

public class RandomSpellingAnomalyReplacer extends SpellingAnomalyReplacer {	
	
	@Override
	protected String getBestReplacement(List<String> replacements) {
		System.out.println("Would have been: "+replacements.toString());
		Collections.shuffle(replacements);
		System.out.println("Is now: "+replacements.toString());
		return replacements.get(0);
	}
}
