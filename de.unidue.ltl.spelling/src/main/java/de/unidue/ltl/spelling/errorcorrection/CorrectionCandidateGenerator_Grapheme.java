package de.unidue.ltl.spelling.errorcorrection;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.Level;

import com.github.liblevenshtein.transducer.Candidate;
import com.github.liblevenshtein.transducer.ITransducer;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.AnnotationChecker;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class CorrectionCandidateGenerator_Grapheme extends CorrectionCandidateGenerator {

	protected final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US.txt";
	protected final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE.txt";

	protected void initializeDefaultDictionary(Set<String> dictionarySet) {
		try {
			BufferedReader br = null;
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
				dictionarySet.add(br.readLine());
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void readAdditionalDictionaries(Set<String> dictionarySet) {
		BufferedReader br = null;
		for (String path : dictionaries) {
			try {
				br = new BufferedReader(new FileReader(new File(path)));
				while (br.ready()) {
					dictionarySet.add(br.readLine());
				}
				br.close();
			} catch (FileNotFoundException e1) {
				getContext().getLogger().log(Level.WARNING, "Custom dictionary " + path + " not found.");
				e1.printStackTrace();
			} catch (IOException e) {
				getContext().getLogger().log(Level.WARNING, "Unable to read custom dictionary " + path);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), Token.class);
		AnnotationChecker.requireExists(this, aJCas, this.getLogger(), SpellingAnomaly.class);

		for (SpellingAnomaly anomaly : select(aJCas, SpellingAnomaly.class)) {

			String tokenText = anomaly.getCoveredText();
			SuggestionCostTuples tuples = new SuggestionCostTuples();

			// Merged word: if we split it, both subparts exist in dictionary

			for (int i = 0; i < tokenText.length(); i++) {
				String word1 = tokenText.substring(0, i);
				String word2 = tokenText.substring(i, tokenText.length());
				if (dictionary.contains(word1) && dictionary.contains(word2)) {
					// System.out.println("Found\t"+tokenText+"\t"+word1+"\t"+word2);
					tuples.addTuple(word1 + " " + word2, 1);
					break;
				}
			}

			if (tokenText.contains(".")) {
				String[] parts = tokenText.split("\\.");
				boolean allFound = true;
				for (String part : parts) {
					if (!dictionary.contains(part) && (!(part.matches("\\d")))) {
						allFound = false;
						break;
					}
				}
				// System.out.println(tokenText+"\t"+allFound);
				if (allFound) {
					String replacement = String.join(" . ", parts);
					replacement = replacement.replaceAll("(\\d) ", "$1");
					tuples.addTuple(replacement, parts.length - 1);
				}
			}

			if (tokenText.contains(":")) {
				String[] parts = tokenText.split(":");
				boolean allFound = true;
				for (String part : parts) {
					if (!dictionary.contains(part) && (!(part.matches("\\d")))) {
						allFound = false;
						break;
					}
				}
				// System.out.println(tokenText+"\t"+allFound);
				if (allFound) {
					String replacement = String.join(" : ", parts);
					replacement = replacement.replaceAll("(\\d) ", "$1");
					tuples.addTuple(replacement, parts.length - 1);
				}
			}

			// Generate candidates

			for (Candidate candidate : transducer.transduce(tokenText, scoreThreshold)) {
				String suggestionString = candidate.term();
				tuples.addTuple(suggestionString, candidate.distance());
			}

			addSuggestedActions(aJCas, anomaly, tuples);
		}
	};
}
