package de.unidue.ltl.spelling.errorcorrection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ErrorDetector extends JCasAnnotator_ImplBase {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	private String[] dictionaries;
	
	//Referring to de.unidue.ltl.spelling.types.Numeric
	public static final String PARAM_EXCLUDE_NUMERIC = "excludeNumeric";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NUMERIC, defaultValue = "true")
	private boolean excludeNumeric;
	
	//Referring to de.unidue.ltl.spelling.types.Punctuation
	public static final String PARAM_EXCLUDE_PUNCTUATION = "excludePunctuation";
	@ConfigurationParameter(name = PARAM_EXCLUDE_PUNCTUATION, defaultValue = "true")
	private boolean excludePunctuation;
	
	//Referring to de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
	public static final String PARAM_EXCLUDE_NAMED_ENTITIES = "excludeNamedEntities";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NAMED_ENTITIES, defaultValue = "true")
	private boolean excludeNamedEntities;

	public static final String PARAM_ADDITIONAL_TYPES_TO_EXCLUDE = "additionalTypesToExclude";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, mandatory = false)
	private String[] additionalTypesToExclude;

	private Set<String> dictionaryWords = new HashSet<String>();
	private Set<String> typesToExclude = new HashSet<String>();
 
	private final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US.txt";
	private final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE_unmunched.txt";
	
	private final String numericType = "de.unidue.ltl.spelling.types.Numeric";
	private final String punctuationType = "de.unidue.ltl.spelling.types.Punctuation";
	private final String namedEntityType = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";

	// Initialize dictionaries & test for conflicts in typesToExclude
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		checkForConflictsInTypesToExclude();
		mergeTypesToExclude();
		try {
			readDefaultDictionary(language);
			readAdditionalDictionaries();
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

	};
	
	private void checkForConflictsInTypesToExclude() {
		
		if(additionalTypesToExclude != null) {
			Set<String> excludeTypes = new HashSet<String>();
			excludeTypes.addAll(Arrays.asList(additionalTypesToExclude));
			if(!excludeNumeric && excludeTypes.contains(numericType)) {
				getContext().getLogger().log(Level.WARNING,
                        "Boolean switch excludeNumeric set to false, but type '" + numericType
                        + "' was passed as type to exclude. Setting excludeNumeric to true.");
				excludeNumeric = true;
			}
			if(!excludePunctuation && excludeTypes.contains(punctuationType)) {
				getContext().getLogger().log(Level.WARNING,
                        "Boolean switch excludePunctuation set to false, but type '" + punctuationType
                        + "' was passed as type to exclude. Setting excludePunctiation to true.");
				excludePunctuation = true;
			}
			if(!excludeNamedEntities && excludeTypes.contains(namedEntityType)) {
				getContext().getLogger().log(Level.WARNING,
                        "Boolean switch excludeNamedEntities set to false, but type '" + namedEntityType
                        + "' was passed as type to exclude. Setting excludeNamedEntities to true.");
				excludeNamedEntities = true;
			}
		}
		
	}
	
	//Combine types that were set to be excluded via parameter with those passed by the user
	private void mergeTypesToExclude() {
		if(additionalTypesToExclude != null) {
			typesToExclude.addAll(Arrays.asList(additionalTypesToExclude));
		}
		if(excludeNumeric) {
			typesToExclude.add(numericType);
		}
		if(excludePunctuation) {
			typesToExclude.add(punctuationType);
		}
		if(excludeNamedEntities) {
			typesToExclude.add(namedEntityType);
		}
	}

	private void readDefaultDictionary(String language) throws IOException {
		BufferedReader br = null;
		if (language.contentEquals("en")) {
			br = new BufferedReader(new FileReader(new File(defaultDictEN)));
		} else if (language.contentEquals("de")) {
			br = new BufferedReader(new FileReader(new File(defaultDictDE)));
		} else {
			getContext().getLogger().log(Level.WARNING,
                    "Unknown language '" + language
                    + "' was passed, defaulting to English dictionary.");
			br = new BufferedReader(new FileReader(new File(defaultDictEN)));
		}

		while (br.ready()) {
			dictionaryWords.add(br.readLine());
		}
		br.close();
	}

	private void readAdditionalDictionaries() throws IOException {
		BufferedReader br = null;
		for (String path : dictionaries) {
			br = new BufferedReader(new FileReader(new File(path)));
			while (br.ready()) {
				dictionaryWords.add(br.readLine());
			}
			br.close();
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		boolean isCandidate = true;
		Collection<Token> tokens = JCasUtil.select(aJCas, Token.class);
		for (Token token : tokens) {
			isCandidate = true;
			// Check if to be excluded
			for (String type : typesToExclude) {
				try {
					if (JCasUtil.contains(aJCas, token, (Class<? extends Annotation>) Class.forName(type))) {
						System.out.println("Marked as ignore: " + token.getCoveredText());
						isCandidate = false;
					}
				} catch (ClassNotFoundException e) {
					getContext().getLogger().log(Level.WARNING,
	                        "Failed to find type '" + type
	                        + "' that was passed as a type to exclude.");
					e.printStackTrace();
				}
			}
			// Check if present in dictionaries
			if (isCandidate) {
				if (!dictionaryWords.contains(token.getCoveredText())) {
					SpellingAnomaly spell = new SpellingAnomaly(aJCas);
					spell.setBegin(token.getBegin());
					spell.setEnd(token.getEnd());
					spell.addToIndexes();
					System.out.println("Found Anomaly: " + token.getCoveredText());
				}
			}

		}

	}

}
