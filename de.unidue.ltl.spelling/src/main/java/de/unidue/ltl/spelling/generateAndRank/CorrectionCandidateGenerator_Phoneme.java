package de.unidue.ltl.spelling.generateAndRank;
//package de.unidue.ltl.spelling.candidategeneration;
//
//import static org.apache.uima.fit.util.JCasUtil.select;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.util.Level;
//import org.dkpro.core.api.parameter.AnnotationChecker;
//
//import com.github.liblevenshtein.transducer.Candidate;
//
//import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
//import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
//import de.unidue.ltl.spelling.utils.PhonemeUtils;
//
//public class CorrectionCandidateGenerator_Phoneme extends CorrectionCandidateGenerator {
//
//	// Tab-separated files containing [grapheme]\t[phoeneme] entries
//	protected final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US_phoneme_map.txt";
//	protected final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE_phoneme_map.txt";
//
//	// A map to look up the set of graphemes matching a phoneme key
//	protected Map<String, Set<String>> phoneme2grapheme = new HashMap<String, Set<String>>();
//
//	protected String getTokenPhoneme(SpellingAnomaly spell) {
//		String lang = null;
//		String result = null;
//		if (language.contentEquals("de")) {
//			lang = "deu-DE";
//		} else if (language.contentEquals("en")) {
//			lang = "eng-US";
//
//		}
//		// Never reached, because SpellingCorrector checks language
//		else {
//			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
//					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
//			System.exit(1);
//		}
//
//		// Obtain phoneme from g2p service
//		try {
//			result = PhonemeUtils.getPhoneme(spell.getCoveredText(), lang);
//		} catch (IOException e) {
//			getContext().getLogger().log(Level.WARNING,
//					"Unable to optain phoneme representation of '" + spell.getCoveredText() + "' from g2p service.");
//			e.printStackTrace();
//		}
//		return result;
//	}
//
//	// Initialize phoneme map and save its keys as the dictionary
//	protected void initializeDefaultDictionary(Set<String> dictionarySet) {
//		String[] entries;
//		try {
//			BufferedReader br = null;
//			Set<String> graphemes;
//			if (language.contentEquals("de")) {
//				br = new BufferedReader(new FileReader(new File(defaultDictDE)));
//
//			} else if (language.contentEquals("en")) {
//				br = new BufferedReader(new FileReader(new File(defaultDictEN)));
//			}
//			// Never reached, because SpellingCorrector checks language
//			else {
//				getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
//						+ "' was passed, as of now only English ('en') and German ('de') are supported.");
//				System.exit(1);
//			}
//
//			while (br.ready()) {
//				entries = br.readLine().split("\t");
//				// Insert grapheme into map under respective phoneme key
//				graphemes = phoneme2grapheme.get(entries[1]);
//				if (graphemes == null) {
//					phoneme2grapheme.put(entries[1], new HashSet<String>());
//					graphemes = phoneme2grapheme.get(entries[1]);
//				}
//				graphemes.add(entries[0]);
//			}
//			br.close();
//			// Keys of the phoneme map = dictionary of phonemes to consider as candidates
//			dictionarySet.addAll(phoneme2grapheme.keySet());
//		} catch (FileNotFoundException e) {
//			getContext().getLogger().log(Level.WARNING,
//					"Unable to locate default phoneme map for language '" + language + "'.");
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	// Additional dictionaries will be ignored if user requested to generate candidates based on phonetic information
//	protected void readAdditionalDictionaries(Set<String> dictionarySet) {
//		if (dictionaries != null) {
//			getContext().getLogger().log(Level.WARNING,
//					"Additional dictionaries were passed, but this is not supported when candidates are generated based on phonetic information. They will not be used.");
//		}
//	}
//
//	@Override
//	public void process(JCas aJCas) throws AnalysisEngineProcessException {
//
//		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), Token.class);
//		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), SpellingAnomaly.class);
//
//		// For each SpellingAnomaly generate phoneme candidates, translate them back to graphemes
//		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {
//
//			// Obtain phonetic representation of anomaly
//			String token = getTokenPhoneme(anomaly);
//			SuggestionCostTuples tuples = new SuggestionCostTuples();
//
//			// Add all graphemes (!) as candidates
//			for (Candidate candidate : transducer.transduce(token, scoreThreshold)) {
//				for (String grapheme : phoneme2grapheme.get(candidate.term())) {
//					tuples.addTuple(grapheme, candidate.distance());
//				}
//			}
//			
//			// Annotate candidates as SuggestedActions for the current SpellingAnomaly
//			addSuggestedActions(aJCas, anomaly, tuples);
//		}
//
//	};
//
//}
