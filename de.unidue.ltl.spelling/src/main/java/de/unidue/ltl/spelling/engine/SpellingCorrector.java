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
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_CustomMatrixes;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_Distance;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_KeyboardDistance;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LanguageModel;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LitKey;
import de.unidue.ltl.spelling.errorcorrection.ErrorDetector;
import de.unidue.ltl.spelling.errorcorrection.ResultTester;
import de.unidue.ltl.spelling.errorcorrection.SpellingAnomalyReplacer;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.FrequencyDistributionLanguageModel;
import de.unidue.ltl.spelling.resources.LanguageModelResource;
import de.unidue.ltl.spelling.resources.Web1TLanguageModel;

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
	
	public static final String PARAM_MATRIX_INSERTION = "levenshteinMatrixInsertion";
	@ConfigurationParameter(name = PARAM_MATRIX_INSERTION, mandatory = false)
	private String filePathInsertion;
	
	public static final String PARAM_MATRIX_DELETION = "levenshteinMatrixDeletion";
	@ConfigurationParameter(name = PARAM_MATRIX_DELETION, mandatory = false)
	private String filePathDeletion;
	
	public static final String PARAM_MATRIX_SUBSTITUTION = "levenshteinMatrixSubstitution";
	@ConfigurationParameter(name = PARAM_MATRIX_SUBSTITUTION, mandatory = false)
	private String filePathSubstitution;
	
	public static final String PARAM_MATRIX_TRANSPOSITION = "levenshteinMatrixTransposition";
	@ConfigurationParameter(name = PARAM_MATRIX_TRANSPOSITION, mandatory = false)
	private String filePathTransposition;

	public static final String PARAM_LANGUAGE_MODEL_PATH = "languageModelPath";
	@ConfigurationParameter(name = PARAM_LANGUAGE_MODEL_PATH, mandatory = false)
	private String languageModelPath;

	public static final String PARAM_CUSTOM_LM_WEIGHT = "customLMWeight";
	@ConfigurationParameter(name = PARAM_CUSTOM_LM_WEIGHT, mandatory = false)
	private double customLMWeight;

	public static final String PARAM_NGRAM_SIZE = "ngramSize";
	@ConfigurationParameter(name = PARAM_NGRAM_SIZE, mandatory = true, defaultValue = "1")
	private int ngramSize;

	public enum CandidateSelectionMethod {
		LEVENSHTEIN_DISTANCE, KEYBOARD_DISTANCE, CUSTOM_MATRIX, PHONETIC, LANGUAGE_MODEL
	}

	private AnalysisEngine spellingCorrectorEngine = null;
	ExternalResourceDescription customLanguageModel;
	ExternalResourceDescription defaultLanguageModel;
	private final String web1tPathGerman = "/Volumes/Marie2/web1t_de/export/data/ltlab/data/web1t/EUROPEAN/data/test";
	private final String web1tPathEnglish = "/Volumes/Marie2/web1t/en/data";

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

		try {
			if (language.contentEquals("de")) {
				defaultLanguageModel = createExternalResourceDescription(Web1TLanguageModel.class,
						Web1TLanguageModel.PARAM_MODEL_FILE, web1tPathGerman);
			} else if (language.contentEquals("en")) {
				defaultLanguageModel = createExternalResourceDescription(Web1TLanguageModel.class,
						Web1TLanguageModel.PARAM_MODEL_FILE, web1tPathEnglish);
			}
			// Will never happen because language has already been checked above
			else {
				getContext().getLogger().log(Level.WARNING, "Unknown language '" + language
						+ "' was passed, as of now only English ('en') and German ('de') are supported.");
				System.exit(1);
			}

			if (languageModelPath != null) {
				System.out.println("ngram size: " + ngramSize);
				customLanguageModel = createExternalResourceDescription(FrequencyDistributionLanguageModel.class,
						FrequencyDistributionLanguageModel.PARAM_MODEL_FILE, languageModelPath,
						FrequencyDistributionLanguageModel.PARAM_NGRAM_SIZE, "" + ngramSize);
			}

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

			// Check whether a LM was provided
			if (customLMWeight > 0 && languageModelPath == null) {
				getContext().getLogger().log(Level.WARNING,
						"You did not provide a custom language model via the 'LANGUAGE_MODEL_PATH' parameter, but passed a weight that should be assigned to this nonexistent language model."
								+ "The weight will be ignored and language model probability will be determined solely based on the default language model.");
			}
			// Check whether the weight is in the expected range
			if (customLMWeight > 1.0) {
				getContext().getLogger().log(Level.WARNING, "You set 'PARAM_CUSTOM_LM_WEIGHT' to " + customLMWeight
						+ ", which is greater than 1. A probability between 0.0 and 1.0 was expected, defaulting to 0.5.");
				customLMWeight = 0.5;
			}

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
			return createEngineDescription(CorrectionCandidateSelector_CustomMatrixes.class,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_DELETION, filePathDeletion,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_INSERTION, filePathInsertion,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_SUBSTITUTION, filePathSubstitution,
					CorrectionCandidateSelector_CustomMatrixes.PARAM_MAP_TRANSPOSITION, filePathTransposition);
		case KEYBOARD_DISTANCE:
			if (candidateSelectionMethod_firstLevel != CandidateSelectionMethod.CUSTOM_MATRIX
					&& candidateSelectionMethod_secondLevel != CandidateSelectionMethod.CUSTOM_MATRIX) {
				if (filePathDeletion != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for deletion was passed as '"
							+ filePathDeletion + ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathInsertion != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for insertion was passed as '"
							+ filePathInsertion + ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathSubstitution != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for substitution was passed as '"
							+ filePathSubstitution + ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
				if (filePathTransposition != null) {
					getContext().getLogger().log(Level.WARNING, "A custom cost matrix for transposition was passed as '"
							+ filePathTransposition + ". This requires selecting CandidateSelectionMethod.CUSTOM_MATRIX to take effect.");
				}
			}
			return createEngineDescription(CorrectionCandidateSelector_KeyboardDistance.class,
					CorrectionCandidateSelector_KeyboardDistance.PARAM_INCLUDE_TRANSPOSITION, includeTransposition,
					CorrectionCandidateSelector_KeyboardDistance.PARAM_LANGUAGE, language);
		case LANGUAGE_MODEL:
			return createEngineDescription(CorrectionCandidateSelector_LanguageModel.class,
					CorrectionCandidateSelector_LanguageModel.PARAM_DEFAULT_LANGUAGE_MODEL, defaultLanguageModel,
					CorrectionCandidateSelector_LanguageModel.PARAM_CUSTOM_LANGUAGE_MODEL, customLanguageModel,
					//TODO: this gives weird type error, says PARAM_CUSTOM_LM_WEIGHT would require float
//					CorrectionCandidateSelector_LanguageModel.PARAM_CUSTOM_LM_WEIGHT, customLMWeight,
					CorrectionCandidateSelector_LanguageModel.PARAM_NGRAM_SIZE, ngramSize);
		case PHONETIC:
			return createEngineDescription(CorrectionCandidateSelector_LitKey.class,
					CorrectionCandidateSelector_LitKey.PARAM_LANGUAGE,language);
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