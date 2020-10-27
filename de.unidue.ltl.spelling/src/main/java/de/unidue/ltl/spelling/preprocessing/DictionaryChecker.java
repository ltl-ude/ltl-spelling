package de.unidue.ltl.spelling.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.frequency.provider.FrequencyCountProvider;

import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.Dictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.LinkingMorphemes;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.SimpleDictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundedWord;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.Fragment;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.LeftToRightSplitterAlgorithm;
import de.unidue.ltl.spelling.types.KnownWord;
import de.unidue.ltl.spelling.types.StartOfSentence;
import de.unidue.ltl.spelling.types.TokenToConsider;

import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Checks presence of tokens within the dictionary that was passed to this
 * Annotator, if present marks them as a KnownWord.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
@TypeCapability(inputs = { "de.unidue.ltl.spelling.types.TokenToConsider" }, outputs = {
		"de.unidue.ltl.spelling.types.KnownWord" })

public class DictionaryChecker extends JCasAnnotator_ImplBase {

//	Language model to resolve compounds (only used for German)
//	TODO: Dummy to put in place in case no model shall be used
	public static final String RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP = "languageModelForCompoundChecking";
	@ExternalResource(key = RES_LANGUAGE_MODEL_FOR_COMPOUND_LOOKUP)
	private FrequencyCountProvider fcp;

	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Path to a dictionary against which to check the TokensToConsider.
	 */
	public static final String PARAM_DICTIONARY_FILE = "dictionaryPath";
	@ConfigurationParameter(name = PARAM_DICTIONARY_FILE, mandatory = true)
	private String dictionaryPath;

	/**
	 * Path to an auxiliary dictionary (optional).
	 */
	public static final String PARAM_AUXILIARY_DICTIONARY_FILE = "auxiliaryDictionaryPath";
	@ConfigurationParameter(name = PARAM_AUXILIARY_DICTIONARY_FILE, mandatory = false)
	private String auxiliaryDictionaryPath;

	/**
	 * Whether to check for compounds (only applicable for German)
	 */
	public static final String PARAM_CHECK_FOR_COMPOUNDS = "checkForCompounds";
	@ConfigurationParameter(name = PARAM_CHECK_FOR_COMPOUNDS, mandatory = true, defaultValue = "false")
	private boolean checkForCompounds;

	public static final String PARAM_LOWERCASE = "lowercase";
	@ConfigurationParameter(name = PARAM_LOWERCASE, mandatory = true, defaultValue = "false")
	private boolean lowercase;

	private Set<String> dictionaryWords = new HashSet<String>();

	LeftToRightSplitterAlgorithm splitter = null;
	LinkingMorphemes linkingMorphemesDE = new LinkingMorphemes(new String[] { "e", "s", "es", "n", "en", "er", "ens" });

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionary(dictionaryPath);
		if (auxiliaryDictionaryPath != null) {
			readDictionary(auxiliaryDictionaryPath);
		}
		if (language.equals("de") && checkForCompounds) {
			initializeCompoundSplitter(dictionaryPath);
		}
	};

	private void readDictionary(String dictionaryPath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(dictionaryPath)));
			while (br.ready()) {
				if (lowercase) {
					dictionaryWords.add(br.readLine().toLowerCase());
				} else {
					dictionaryWords.add(br.readLine());
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initializeCompoundSplitter(String dictionaryPath) {
		Dictionary dict = new SimpleDictionary(Paths.get(dictionaryPath).toFile());
		splitter = new LeftToRightSplitterAlgorithm();
		splitter.setDictionary(dict);
		splitter.setLinkingMorphemes(linkingMorphemesDE);
		splitter.setMinWordLength(3);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (TokenToConsider consider : JCasUtil.select(aJCas, TokenToConsider.class)) {

//			If token is beginning of a new sentence: process lowercased as well
			if (!JCasUtil.selectCovered(StartOfSentence.class, consider).isEmpty()) {
//				System.out.println("Also checking in lowercase, because\t" + consider.getCoveredText() + "\t is BOS");
				checkIfWordIsKnown(aJCas, consider, true);
			} else {
				checkIfWordIsKnown(aJCas, consider, false);
			}
		}
	}

	private void checkIfWordIsKnown(JCas aJCas, TokenToConsider token, boolean isBeginningOfSentence) {
		String currentWord = token.getCoveredText();
		if (lowercase) {
			currentWord = currentWord.toLowerCase();
		}
		if (dictionaryWords.contains(currentWord)
				|| (isBeginningOfSentence && dictionaryWords.contains(currentWord.toLowerCase()))) {
			KnownWord word = new KnownWord(aJCas);
			word.setPathToDictItWasFoundIn(dictionaryPath);
			word.setBegin(token.getBegin());
			word.setEnd(token.getEnd());
			word.addToIndexes();
//			System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(found in " + dictionaryPath + ")");
		} else if (language.equals("it") && currentWord.contains("'")) {

			String[] wordParts = currentWord.split("'");
			if (wordParts.length == 2) {
				if ((dictionaryWords.contains(wordParts[0]) && dictionaryWords.contains(wordParts[1]))
						|| (isBeginningOfSentence && dictionaryWords.contains(wordParts[0].toLowerCase())
								&& dictionaryWords.contains(wordParts[1]))) {
					KnownWord word = new KnownWord(aJCas);
					word.setPathToDictItWasFoundIn(dictionaryPath);
					word.setBegin(token.getBegin());
					word.setEnd(token.getEnd());
					word.addToIndexes();
//					System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(found in " + dictionaryPath + ")");
				}
			}
//		Strip punctuation from beginning and end of token
		} else if (currentWord.matches("^([\\W\\s]+?)([\\w\\u0080-\\uFFFF]+)([\\W\\s]*?)$")
				|| currentWord.matches("^([\\W\\s]*?)([\\w\\u0080-\\uFFFF]+)([\\W\\s]+?)$")) {
			String stripNonAlpha = currentWord.replaceAll("^([\\W\\s]*?)([\\w\\u0080-\\uFFFF]+)([\\W\\s]*?)$", "$2");
			if (dictionaryWords.contains(stripNonAlpha)
					|| isBeginningOfSentence && dictionaryWords.contains(stripNonAlpha.toLowerCase())) {
				KnownWord word = new KnownWord(aJCas);
				word.setPathToDictItWasFoundIn(dictionaryPath);
				word.setBegin(token.getBegin());
				word.setEnd(token.getEnd());
				word.addToIndexes();
//				System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(found in " + dictionaryPath + "as "+ stripNonAlpha+")");
			}
		}
		// Only for German: if splitter finds a compound: is also a KnownWord.
		else if (language.equals("de") && checkForCompounds) {

			// errors where a space was omitted ("heuteAbend") are returned
			// as compounds, therefore include LM probability check: only accept compound if
			// it is less probable to be observed separated than written as a compound

			if (splitter.split(currentWord).getSplits().get(0).isCompound()) {

				double freqAsCompound = 0;
				try {
//					System.out.println(currentWord + "\tin LM:\t" + fcp.getFrequency(currentWord));
					freqAsCompound = fcp.getFrequency(currentWord);
				} catch (IOException e) {
					e.printStackTrace();
				}

				boolean isCompound = true;

				for (DecompoundedWord split : splitter.split(currentWord).getSplits()) {
					String phraseToCheck = "";

					for (Fragment subword : split.getSplits()) {
						String word = subword.getWordWithMorpheme();
//						System.out.println(subword.getWord());
						if (!currentWord.contains(word)) {
							// Assume it is uppercased in the original word
							word = word.substring(0, 1).toUpperCase() + word.substring(1);
						}
						phraseToCheck = phraseToCheck + " " + word;
					}

					double freqDecompounded = 0;
					try {
//						System.out.println(phraseToCheck + "\tin LM:\t" + fcp.getFrequency(phraseToCheck));
						freqDecompounded = fcp.getFrequency(phraseToCheck);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if (freqAsCompound <= freqDecompounded) {
						isCompound = false;
					}
				}

				if (isCompound) {

					KnownWord word = new KnownWord(aJCas);
					word.setBegin(token.getBegin());
					word.setEnd(token.getEnd());
					word.addToIndexes();
//					System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(compound of words found in "
//							+ dictionaryPath + ")");
				}
//				else {
//					System.out.println("Did consider, but is not a compound: " + currentWord);
//				}
			}
		}
	}
}