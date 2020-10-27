package de.unidue.ltl.spelling.reader;

import java.util.HashMap;
import java.util.Map;

public class SpellingItem {

	private String corpusName;
	private String id;
	private String text;
	Map<String, String> corrections = new HashMap<String, String>();
	Map<String, String> correctionErrorTypes = new HashMap<String, String>();
	Map<String, String> grammarCorrections = new HashMap<String, String>();

	public String getText() {
		return this.text;
	}

	public String getId() {
		return this.id;
	}

	public String getCorpusName() {
		return this.corpusName;
	}

	public Map<String, String> getCorrections() {
		return this.corrections;
	}
	
	public Map<String, String> getCorrectionErrorTypes() {
		return this.correctionErrorTypes;
	}

	public Map<String, String> getGrammarCorrections() {
		return this.grammarCorrections;
	}

	public SpellingItem(String corpusName, String id, String text, Map<String, String> correctionMap,
			Map<String, String> correctionErrorTypes, Map<String, String> grammarCorrectionMap) {
		this.text = text;
		this.id = id;
		this.corpusName = corpusName;
		this.corrections = correctionMap;
		this.correctionErrorTypes = correctionErrorTypes;
		this.grammarCorrections = grammarCorrectionMap;
	}
}