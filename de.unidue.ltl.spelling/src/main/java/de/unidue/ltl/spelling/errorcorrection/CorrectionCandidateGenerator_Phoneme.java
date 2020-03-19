package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.Level;

import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.AnnotationChecker;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.utils.PhonemeUtils;

public class CorrectionCandidateGenerator_Phoneme extends CorrectionCandidateGenerator {

	protected final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US_phoneme_map.txt";
	protected final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt";

	protected Map<String, Set<String>> phoneme2grapheme = new HashMap<String, Set<String>>();

	protected String getTokenPhoneme(SpellingAnomaly spell) {
		String lang = null;
		String result = null;
		if (language.contentEquals("de")) {
			lang = "deu-DE";
		} else if (language.contentEquals("en")) {
			lang = "eng-US";
			// Never reached, because SpellingCorrector checks language
		} else {
			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
			System.exit(1);
		}

		try {
			result = PhonemeUtils.getPhoneme(spell.getCoveredText(), lang);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// Create map and derive dictionary from its keys
	protected void initializeDefaultDictionary(Set<String> dictionarySet) {
		String[] entries;
		try {
			BufferedReader br = null;
			Set<String> graphemes;
			if (language.contentEquals("de")) {
				br = new BufferedReader(new FileReader(new File(defaultDictDE)));

			} else if (language.contentEquals("en")) {
				br = new BufferedReader(new FileReader(new File(defaultDictEN)));
			}
			// Never reached, because SpellingCorrector checks language
			else {
				getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
						+ "' was passed, as of now only English ('en') and German ('de') are supported.");
				System.exit(1);
			}
			while (br.ready()) {
				entries = br.readLine().split("\t");
				graphemes = phoneme2grapheme.get(entries[1]);
				if (graphemes == null) {
					phoneme2grapheme.put(entries[1], new HashSet<String>());
					graphemes = phoneme2grapheme.get(entries[1]);
				}
				graphemes.add(entries[0]);
			}
			br.close();
			dictionarySet.addAll(phoneme2grapheme.keySet());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void readAdditionalDictionaries(Set<String> dictionarySet) {
		if (dictionaries != null) {
			getContext().getLogger().log(Level.WARNING,
					"Additional dictionaries were passed, but this is not supported when candidates are generated based on phonetic information. They will not be used.");
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), Token.class);
		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), SpellingAnomaly.class);

		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {

			String token = getTokenPhoneme(anomaly);
			SuggestionCostTuples tuples = new SuggestionCostTuples();

			// Generate candidates, add a tuple for all graphemes corresponding to the
			// respective phonemes
			for (Candidate candidate : transducer.transduce(token, scoreThreshold)) {
				for (String grapheme : phoneme2grapheme.get(candidate.term())) {
					tuples.addTuple(grapheme, candidate.distance());
				}
			}
			addSuggestedActions(aJCas, anomaly, tuples);
		}

	};

}
