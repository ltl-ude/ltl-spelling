package de.unidue.ltl.spelling.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
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
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Language;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.Dictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.dictionary.SimpleDictionary;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.JWordSplitterAlgorithm;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.KnownWord;
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
	JWordSplitterAlgorithm splitter = null;

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
		splitter = new JWordSplitterAlgorithm();
		splitter.setDictionary(dict);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		System.out.println();
		for (TokenToConsider consider : JCasUtil.select(aJCas, TokenToConsider.class)) {
			String currentWord = consider.getCoveredText();
			// Word itself is present in dictionary.
			if (dictionaryWords.contains(currentWord)) {
				KnownWord word = new KnownWord(aJCas);
				word.setBegin(consider.getBegin());
				word.setEnd(consider.getEnd());
				word.addToIndexes();
				System.out.println(
						"Marked as known:\t" + consider.getCoveredText() + "\t(found in " + dictionaryPath + ")");
			}
			// Only for German: if splitter finds a compound: is also a KnownWord.
			else if (language.equals("de") && splitter.split(currentWord).getSplits().get(0).isCompound()) {
				KnownWord word = new KnownWord(aJCas);
				word.setBegin(consider.getBegin());
				word.setEnd(consider.getEnd());
				word.addToIndexes();
				System.out.println("Marked as known:\t" + consider.getCoveredText() + "\t(compound of words found in "
						+ dictionaryPath + ")");
			}
		}
	}

}