package de.unidue.ltl.spelling.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * In case of multiple candidates with an equally well score, pick one at random
 */

public class RandomSpellingAnomalyReplacer extends SpellingAnomalyReplacer {	
	
	@Override
	protected Entry<String,String> getBestReplacement(Map<String,String> replacements) {
		List<String> keys = new ArrayList<String>();
		keys.addAll(replacements.keySet());
		Collections.shuffle(keys);
		String correction = keys.get(0);
		for(Entry<String, String> entry : replacements.entrySet()) {
			if(entry.getKey().equals(correction)){
				return entry;
			}
		}
		return null;
	}
}
