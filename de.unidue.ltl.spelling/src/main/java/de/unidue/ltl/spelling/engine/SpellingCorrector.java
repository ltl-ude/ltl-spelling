package de.unidue.ltl.spelling.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.dkpro.core.castransformation.ApplyChangesAnnotator;
import org.dkpro.core.castransformation.Backmapper;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.unidue.ltl.spelling.errorcorrection.ApplyChanges;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator_Grapheme;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateGenerator_Phoneme;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_Distance;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LanguageModel;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_LitKey;
import de.unidue.ltl.spelling.errorcorrection.CorrectionCandidateSelector_Matrix;
import de.unidue.ltl.spelling.errorcorrection.ErrorDetector;
import de.unidue.ltl.spelling.errorcorrection.ResultTester;
import de.unidue.ltl.spelling.preprocessing.NumericAnnotator;
import de.unidue.ltl.spelling.preprocessing.PunctuationAnnotator;
import de.unidue.ltl.spelling.resources.LanguageModelResource;

public class SpellingCorrector extends JCasAnnotator_ImplBase {

	// For Error Detector

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	// Same dictionary will also be passed to candidate generation
	public static final String PARAM_ADDITIONAL_DICTIONARIES = "dictionaries";
	@ConfigurationParameter(name = PARAM_ADDITIONAL_DICTIONARIES, mandatory = false)
	private String[] dictionaries;

	// Referring to de.unidue.ltl.spelling.types.Numeric
	public static final String PARAM_PHONETIC_CANDIDATE_GENERATION = "phoneticCandidateGeneration";
	@ConfigurationParameter(name = PARAM_PHONETIC_CANDIDATE_GENERATION, mandatory = true, defaultValue = "false")
	private boolean phoneticCandidateGeneration;

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

	// For Candidate Generation (classic Levenshtein or phonetic)

	public static final String PARAM_MODEL_ENCODING = ComponentParameters.PARAM_MODEL_ENCODING;
	@ConfigurationParameter(name = PARAM_MODEL_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String dictEncoding;

	public static final String PARAM_SCORE_THRESHOLD = "ScoreThreshold";
	@ConfigurationParameter(name = PARAM_SCORE_THRESHOLD, mandatory = true, defaultValue = "1")
	private int scoreThreshold;

	// For Candidate Selection

	public static final String PARAM_SELECTION_METHOD = "CandidateSelectionMethod";
	@ConfigurationParameter(name = PARAM_SELECTION_METHOD, mandatory = true, defaultValue = "LEVENSHTEIN_UNIFORM")
	protected CandidateSelectionMethod candidateSelectionMethod;

	// For Normalization
	// TODO: this does not have to be accessible from the outside does it?
	// public static final String PARAM_TYPES_TO_COPY = "typesToCopy";
	// @ConfigurationParameter(name = PARAM_TYPES_TO_COPY, mandatory = true)

	// public static final String PARAM_LANGUAGE_MODEL = "languageModel";
	// @ExternalResource(key=PARAM_LANGUAGE_MODEL)
	// private LanguageModelResource languageModel;

	// For language model
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

		ExternalResourceDescription languageModel = createExternalResourceDescription(LanguageModelResource.class,
				LanguageModelResource.PARAM_MODEL_FILE, languageModelPath);

		try {
			// Preprocessing
			AnalysisEngineDescription segmenter = createEngineDescription(CoreNlpSegmenter.class);
			AnalysisEngineDescription numericAnnotator = createEngineDescription(NumericAnnotator.class);
			AnalysisEngineDescription punctuationAnnotator = createEngineDescription(PunctuationAnnotator.class);
			AnalysisEngineDescription namedEntityAnnotator = createEngineDescription(
					StanfordNamedEntityRecognizer.class);

			// Error Detection
			AnalysisEngineDescription errorDetector = createEngineDescription(ErrorDetector.class,
					ErrorDetector.PARAM_ADDITIONAL_DICTIONARIES, dictionaries,
					ErrorDetector.PARAM_ADDITIONAL_TYPES_TO_EXCLUDE, additionalTypesToExclude,
					ErrorDetector.PARAM_LANGUAGE, language);

			AnalysisEngineDescription candidateGenerator;

			if (phoneticCandidateGeneration) {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Grapheme.class,
						CorrectionCandidateGenerator_Grapheme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Grapheme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Grapheme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			} else {
				candidateGenerator = createEngineDescription(CorrectionCandidateGenerator_Phoneme.class,
						CorrectionCandidateGenerator_Phoneme.PARAM_DISTANCE_THRESHOLD, scoreThreshold,
						CorrectionCandidateGenerator_Phoneme.PARAM_LANGUAGE, language,
						CorrectionCandidateGenerator_Phoneme.PARAM_ADDITIONAL_DICTIONARIES, dictionaries);
			}

			// Normalization
			AnalysisEngineDescription changeAnnotator;

			switch (candidateSelectionMethod) {
			case LEVENSHTEIN_DISTANCE:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_Distance.class,
						CorrectionCandidateSelector_Distance.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			case CUSTOM_MATRIX:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_Matrix.class,
						CorrectionCandidateSelector_Matrix.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			case KEYBOARD_DISTANCE:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_Matrix.class,
						CorrectionCandidateSelector_Matrix.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			case LANGUAGE_MODEL_FREQUENCY:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_LanguageModel.class,
						CorrectionCandidateSelector_LanguageModel.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			case LANGUAGE_MODEL_PROBABILITY:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_LanguageModel.class,
						CorrectionCandidateSelector_LanguageModel.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			case PHONETIC:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_LitKey.class,
						CorrectionCandidateSelector_LitKey.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			default:
				changeAnnotator = createEngineDescription(CorrectionCandidateSelector_Distance.class,
						CorrectionCandidateSelector_Distance.PARAM_TYPES_TO_COPY, new String[] {
								"de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly" });
				break;
			}

			AnalysisEngineDescription changeApplier = createEngineDescription(ApplyChanges.class);
			AnalysisEngineDescription resultTester = createEngineDescription(ResultTester.class);

			List<AnalysisEngineDescription> spellingComponents = new ArrayList<AnalysisEngineDescription>();
			spellingComponents.add(segmenter);
			spellingComponents.add(numericAnnotator);
			spellingComponents.add(punctuationAnnotator);
			spellingComponents.add(namedEntityAnnotator);
			spellingComponents.add(errorDetector);
			spellingComponents.add(candidateGenerator);
			spellingComponents.add(changeAnnotator);
			spellingComponents.add(changeApplier);
			spellingComponents.add(segmenter);
//				spellingComponents.add(backmapper);
			spellingComponents.add(resultTester);

			List<String> componentNames = new ArrayList<String>();
			componentNames.add("segmenter");
			componentNames.add("numericAnnotator");
			componentNames.add("punctuationAnnotator");
			componentNames.add("namedEntityAnnotator");
			componentNames.add("errorDetector");
			componentNames.add("candidateGenerator");
			componentNames.add("changeAnnotator");
			componentNames.add("changeApplier");
			componentNames.add("segmenter");
//				componentNames.add("backmapper");
			componentNames.add("resultTester");

			spellingCorrectorEngine = AnalysisEngineFactory.createEngine(spellingComponents, componentNames, null,
					null);

		} catch (ResourceInitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		spellingCorrectorEngine.process(aJCas);

	}

}
