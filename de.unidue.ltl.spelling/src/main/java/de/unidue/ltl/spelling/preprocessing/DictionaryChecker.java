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
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.Dictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.LinkingMorphemes;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.SimpleDictionary;
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

	/**
	 * Language of processed content.
	 */
	public static final String PARAM_LANGUAGE = "language";
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Path to a dictionary against which to check the TokensToConsider. Must
	 * contain one word per line.
	 */
	public static final String PARAM_DICTIONARY_FILE = "dictionaryPath";
	@ConfigurationParameter(name = PARAM_DICTIONARY_FILE, mandatory = true)
	private String dictionaryPath;

	private Set<String> dictionaryWords = new HashSet<String>();

	// If German is being processed, check for compounds
	LeftToRightSplitterAlgorithm splitter = null;
	LinkingMorphemes linkingMorphemesDE = new LinkingMorphemes(new String[] { "e", "s", "es", "n", "en", "er", "ens" });

//	private final String defaultDictEN = "src/main/resources/dictionaries/hunspell_en_US.txt";
//	private final String defaultDictDE = "src/main/resources/dictionaries/hunspell_DE.txt";

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		readDictionary(dictionaryPath);
		if (language.equals("de")) {
			initializeCompoundSplitter(dictionaryPath);
		}
	};

	private void readDictionary(String dictionaryPath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(dictionaryPath)));
			while (br.ready()) {
				dictionaryWords.add(br.readLine());
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
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		System.out.println();
		for (TokenToConsider consider : JCasUtil.select(aJCas, TokenToConsider.class)) {

			// If token is beginning of a new sentence: process in lowercase as well
			if (!JCasUtil.selectCovered(StartOfSentence.class, consider).isEmpty()) {
				System.out.println("Also checking in lowercase, because\t" + consider.getCoveredText() + "\t is BOS");
				checkIfWordIsKnown(aJCas, consider, true);
			} else {
				checkIfWordIsKnown(aJCas, consider, false);
			}
		}
	}

	private void checkIfWordIsKnown(JCas aJCas, TokenToConsider token, boolean alsoCheckLowercase) {
		String currentWord = token.getCoveredText();
		if (dictionaryWords.contains(currentWord)
				|| (alsoCheckLowercase && dictionaryWords.contains(currentWord.toLowerCase()))) {
			KnownWord word = new KnownWord(aJCas);
			word.setBegin(token.getBegin());
			word.setEnd(token.getEnd());
			word.addToIndexes();
			System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(found in " + dictionaryPath + ")");
		}
		// Only for German: if splitter finds a compound: is also a KnownWord.
		else if (language.equals("de") && splitter.split(currentWord).getSplits().get(0).isCompound()) {

			// TODO: as of now, errors where a space was omitted ("heuteAbend") are marked
			// as compounds, therefore include LM probability check: only accept compound if
			// it is less probable to be observed separated than written as a compound

//			System.out.println(
//					"Found\t" + splitter.split(currentWord).getSplits().size() + "\t splits for\t" + currentWord);
//			System.out.println(
//					"Example split of\t" + currentWord + "\tis\t" + splitter.split(currentWord).getSplits().get(0));

			KnownWord word = new KnownWord(aJCas);
			word.setBegin(token.getBegin());
			word.setEnd(token.getEnd());
			word.addToIndexes();
			System.out.println("Marked as known:\t" + token.getCoveredText() + "\t(compound of words found in "
					+ dictionaryPath + ")");
		}
	}
}