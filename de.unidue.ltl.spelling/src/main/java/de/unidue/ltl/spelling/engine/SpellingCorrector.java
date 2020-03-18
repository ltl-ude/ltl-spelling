package de.unidue.ltl.spelling.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.errorcorrection.ApplyChanges;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator_Grapheme;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator_Phoneme;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_Distance;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LanguageModelFrequency;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LanguageModelProbability;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LitKey;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_Matrix;
import de.unidue.ltl.spelling.errorcorrection.ErrorDetector;
import de.unidue.ltl.spelling.errorcorrection.ResultTester;
import de.unidue.ltl.spelling.errorcorrection.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class SpellingCorrector extends JCasAnnotator_ImplBase {

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	// For ErrorDetector

	// Same dictionary will also be passed to candidate generation
	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	private String[] dictionaries;

	// Referring to de.unidue.ltl.spelling.types.Numeric
	public static final String PARAM_EXCLUDE_NUMERIC = "excludeNumeric";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NUMERIC, mandatory = true, defaultValue = "true")
	private boolean excludeNumeric;

	// Referring to de.unidue.ltl.spelling.types.Punctuation
	public static final String PARAM_EXCLUDE_PUNCTUATION = "excludePunctuation";
	@ConfigurationParameter(name = PARAM_EXCLUDE_PUNCTUATION, mandatory = true, defaultValue = "true")
	private boolean excludePunctuation;

	// Referring to de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity
	public static final String PARAM_EXCLUDE_NAMED_ENTITIES = "excludeNamedEntities";
	@ConfigurationParameter(name = PARAM_EXCLUDE_NAMED_ENTITIES, mandatory = true, defaultValue = "true")
	private boolean excludeNamedEntities;

	public static final String PARAM_ADDITIONAL_TYPES_TO_EXCLUDE = "additionalTypesToExclude";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, mandatory = false)
	private String[] additionalTypesToExclude;

	// For candidate generation

	public static final String PARAM_INCLUDE_TRANSPOSITION = "includeTransposition";
	@ConfigurationParameter(name = PARAM_INCLUDE_TRANSPOSITION, mandatory = true, defaultValue = "false")
	private boolean includeTransposition;

	public static final String PARAM_PHONETIC_CANDIDATE_GENERATION = "phoneticCandidateGeneration";
	@ConfigurationParameter(name = PARAM_PHONETIC_CANDIDATE_GENERATION, mandatory = true, defaultValue = "false")
	private boolean phoneticCandidateGeneration;

	public static final String PARAM_SCORE_THRESHOLD = "scoreThreshold";
	@ConfigurationParameter(name = PARAM_SCORE_THRESHOLD, mandatory = true, defaultValue = "1")
	private int scoreThreshold;

	// For candidate selection

	public static final String PARAM_FIRST_LEVEL_SELECTION_METHOD = "candidateSelectionMethod_firstLevel";
	@ConfigurationParameter(name = PARAM_FIRST_LEVEL_SELECTION_METHOD, mandatory = true, defaultValue = "LEVENSHTEIN_DISTANCE")
	protected CandidateSelectionMethod candidateSelectionMethod_firstLevel;

	public static final String PARAM_SECOND_LEVEL_SELECTION_METHOD = "candidateSelectionMethod_secondLevel";
	@ConfigurationParameter(name = PARAM_SECOND_LEVEL_SELECTION_METHOD, mandatory = false)
	protected CandidateSelectionMethod candidateSelectionMethod_secondLevel;

	public static final String PARAM_LANGUAGE_MODEL_PATH = "languageModelPath";
	@ConfigurationParameter(name = PARAM_LANGUAGE_MODEL_PATH, mandatory = true)
	private String languageModelPath;

	public enum CandidateSelectionMethod {
		LEVENSHTEIN_DISTANCE, KEYBOARD_DISTANCE, CUSTOM_MATRIX, PHONETIC, LANGUAGE_MODEL_FREQUENCY,
		LANGUAGE_MODEL_PROBABILITY
	}

	private AnalysisEngine spellingCorrectorEngine = null;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

		// To ensure that parameters of this class have been set to be able to use them
		// in creating engines below
		super.initialize(context);

		if (!(language.contentEquals("de") || language.contentEquals("en"))) {
			getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
					+ "' was passed, as of now only English ('en') and German ('de') are supported.");
			System.exit(1);
		}

//		ExternalResourceDescription languageModel = createExternalResourceDescription(LanguageModelResource.class,
//				LanguageModelResource.PARAM_MODEL_FILE, languageModelPath);

		try {

			List<AnalysisEngineDescription> spellingComponents = new ArrayList<AnalysisEngineDescription>();
			List<String> componentNames = new ArrayList<String>();

			// Preprocessing
			AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
			spellingComponents.add(segmenter);
			componentNames.add("segmenter");

			AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
			spellingComponents.add(numericAnnotator);
			componentNames.add("numericAnnotator");

			AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
			spellingComponents.add(punctuationAnnotator);
			componentNames.add("punctuationAnnotator");

			AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(
					StanfordNamedEntityRecognizer.class);
			spellingComponents.add(namedEntityAnnotator);
			componentNames.add("namedEntityAnnotator");

			// Error Detection
			AnalysisEngineDescription errorDetector = createEngineDescription(ErrorDetector.class,
					ErrorDetector.PARAM_ADDITIONAL_DICTIONARIES, dictionaries,
					ErrorDetector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, additionalTypesToExclude,
					ErrorDetector.PARAM_LANGUAGE, language);
			spellingComponents.add(errorDetector);
			componentNames.add("errorDetector");

			// Candidate Generation
			AnalysisEngineDescription candidateGenerator;
			if (!phoneticCandidateGeneration) {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Grapheme.class,
						CorrectionCandidateGenerator_Grapheme.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
						CorrectionCandidateGenerator_Grapheme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Grapheme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Grapheme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			} else {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Phoneme.class,
						CorrectionCandidateGenerator_Phoneme.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
						CorrectionCandidateGenerator_Phoneme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Phoneme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Phoneme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			}
			spellingComponents.add(candidateGenerator);
			componentNames.add("candidateGenerator");

			// CandidateSelection
			AnalysisEngineDescription candidateSelector_firstLevel = getCandidateSelector(
					candidateSelectionMethod_firstLevel);
			spellingComponents.add(candidateSelector_firstLevel);
			componentNames.add("candidateSelector_firstLevel");

			if (candidateSelectionMethod_secondLevel != null) {
				AnalysisEngineDescription candidateSelector_secondLevel = getCandidateSelector(
						candidateSelectionMethod_secondLevel);
				spellingComponents.add(candidateSelector_secondLevel);
				componentNames.add("candidateSelector_secondLevel");
			}

			// Normalization
			AnalysisEngineDescription changeAnnotator = createEngineDescription(SpellingAnomalyReplacer.class,
					SpellingAnomalyReplacer.PARAM_TYPES_TO_COPY,
					new String[] { "de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
			spellingComponents.add(changeAnnotator);
			componentNames.add("changeAnnotator");

			AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
			spellingComponents.add(changeApplier);
			componentNames.add("changeApplier");

			// Repeat tokenization to ensure correct tokens after replacements of
			// misspellings may have introduced blank spaces within tokens
			spellingComponents.add(segmenter);
			componentNames.add("segmenter");

			AnalysisEngineDescription resultTester = createEngineDescription(ResultTester.class);
			spellingComponents.add(resultTester);
			componentNames.add("resultTester");

			spellingCorrectorEngine = AnalysisEngineFactory.createEngine(spellingComponents, componentNames, null,
					null);

		} catch (ResourceInitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private AnalysisEngineDescription getCandidateSelector(CandidateSelectionMethod method)
			throws ResourceInitializationException {
		switch (method) {
		case LEVENSHTEIN_DISTANCE:
			return createEngineDescription(CorrectionCandidateSelector_Distance.class);
		case CUSTOM_MATRIX:
			return createEngineDescription(CorrectionCandidateSelector_Matrix.class);
		case KEYBOARD_DISTANCE:
			return createEngineDescription(CorrectionCandidateSelector_Matrix.class);
		case LANGUAGE_MODEL_FREQUENCY:
			return createEngineDescription(CorrectionCandidateSelector_LanguageModelProbability.class);
		case LANGUAGE_MODEL_PROBABILITY:
			return createEngineDescription(CorrectionCandidateSelector_LanguageModelFrequency.class);
		case PHONETIC:
			return createEngineDescription(CorrectionCandidateSelector_LitKey.class);
		default:
			getContext().getLogger().log(Level.WARNING,
					"Selected unknown selection type '" + method + "' , defaulting to levenshtein distance.");
			return createEngineDescription(CorrectionCandidateSelector_Distance.class);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		spellingCorrectorEngine.process(aJCas);
	}

}