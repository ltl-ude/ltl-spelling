package de.unidue.ltl.spelling.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.uimafit.util.JCasUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly;
import de.unidue.ltl.spelling.types.GrammarError;
import de.unidue.ltl.spelling.types.SpellingError;
import de.unidue.ltl.spelling.types.SpellingText;

public class SpellingReader extends JCasResourceCollectionReader_ImplBase {

	/**
	 * Encoding
	 */
	public static final String PARAM_ENCODING = "Encoding";
	@ConfigurationParameter(name = PARAM_ENCODING, mandatory = false, defaultValue = "UTF-8")
	public String encoding;

	/**
	 * Language
	 */
	public static final String PARAM_LANGUAGE_CODE = "Language";
	@ConfigurationParameter(name = PARAM_LANGUAGE_CODE, mandatory = true)
	private String language;

	public static final String PARAM_SOURCE_FILE = "sourceFile";
	@ConfigurationParameter(name = PARAM_SOURCE_FILE, mandatory = true)
	protected String sourceFile;

	public static final String PARAM_FOR_ERROR_DETECTION = "forErrorDetection";
	@ConfigurationParameter(name = PARAM_FOR_ERROR_DETECTION, mandatory = true, defaultValue = "false")
	protected boolean errorDetection;

	protected int currentIndex;
	Queue<SpellingItem> items;
	int numberOfTexts = -1;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		items = new LinkedList<SpellingItem>();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		
		try {
			builder = builderFactory.newDocumentBuilder();

			int nrOfErrors = 0;
			int nrOfGrammarErrors = 0;
			Document document = builder.parse(new FileInputStream(sourceFile));
			Element rootElement = document.getDocumentElement();
			NodeList nodes = rootElement.getChildNodes();
			String corpusName = rootElement.getAttribute("name");
			System.out.println("corpus name: " + corpusName);

			for (int i = 0; i < nodes.getLength(); i++) {

				int endIndex = 0;
				Map<String, String> correctionMap = new HashMap<String, String>();
				Map<String, String> grammarCorrectionMap = new HashMap<String, String>();

				Node node = nodes.item(i);

				if (node instanceof Element) {
					// a child element to process
					Element child = (Element) node;
					String id = child.getAttribute("id");
//					System.out.println("Text ID: " + id);

					String text = child.getTextContent();
					System.out.println("Text: " + text);

					NodeList errors = child.getElementsByTagName("error");
					
					NodeList children = child.getChildNodes(); 
						
					for(int e = 0; e<children.getLength(); e++) {
						
						Node childNode = children.item(e);
//						System.out.println(childNode.getNodeName());
						if(childNode.getNodeName().equals("error")) {
							nrOfErrors++;
							String misspelling = childNode.getTextContent();
							String correction = ((Element) childNode).getAttribute("correct");
//							System.out.println("error:\t" + misspelling);
//							System.out.println("correction:\t" + correction);
							
							int startIndex = endIndex;
							endIndex += misspelling.length();
							correctionMap.put(startIndex + "-" + endIndex, correction);
							System.out.println(startIndex + "\tto\t" + endIndex + ":\t" + correction + "\t(was "
									+ text.substring(startIndex, endIndex) + ")");
						}
						else if(childNode.getNodeName().equals("grammar_error")) {
							nrOfGrammarErrors++;
							String misspelling = childNode.getTextContent();
							String correction = ((Element) childNode).getAttribute("correct");
//							System.out.println("error:\t" + misspelling);
//							System.out.println("correction:\t" + correction);
							
							int startIndex = endIndex;
							endIndex += misspelling.length();
							grammarCorrectionMap.put(startIndex + "-" + endIndex, correction);
							System.out.println("Grammar: "+startIndex + "\tto\t" + endIndex + ":\t" + correction + "\t(was "
									+ text.substring(startIndex, endIndex) + ")");
						}
						else {
							endIndex += childNode.getTextContent().length();
						}
						
						
					}
					

//					for (int e = 0; e < errors.getLength(); e++) {
//
//						nrOfErrors++;
//						Node error = errors.item(e);
//
//						// a child element to process
//						String misspelling = error.getTextContent();
//						String correction = ((Element) error).getAttribute("correct");
//						System.out.println("error:\t" + misspelling);
//						System.out.println("correction:\t" + correction);
//
//						int addToIndex = 0;
//						
//						try {
//							addToIndex = error.getPreviousSibling().getTextContent().length();
//							System.out.println("prev sibling: "+error.getPreviousSibling().getTextContent());
//						} catch (NullPointerException n) {
//							// If no sibling can be retrieved: error directly at beginning of sentence,
//							// addToIndex=0
//						}
//
//						endIndex += addToIndex;
//						int startIndex = endIndex;
//						endIndex += misspelling.length();
//
//						correctionMap.put(startIndex + "-" + endIndex, correction);
//						System.out.println(startIndex + "\tto\t" + endIndex + ":\t" + correction + "\t(was "
//								+ text.substring(startIndex, endIndex) + ")");
//
//					}
//
//					NodeList grammarErrors = child.getElementsByTagName("grammar_error");
//					endIndex=0;
//
//					for (int e = 0; e < grammarErrors.getLength(); e++) {
//
//						nrOfGrammarErrors++;
//						Node error = grammarErrors.item(e);
//
//						// a child element to process
//						String misspelling = error.getTextContent();
//						String correction = ((Element) error).getAttribute("correct");
////						System.out.println("error:\t" + misspelling);
////						System.out.println("correction:\t" + correction);
//
//						int addToIndex = 0;
//						try {
//							addToIndex = error.getPreviousSibling().getTextContent().length();
//						} catch (NullPointerException n) {
//							// If no sibling can be retrieved: error directly at beginning of sentence,
//							// addToIndex=0
//						}
//
//						endIndex += addToIndex;
//						int startIndex = endIndex;
//						endIndex += misspelling.length();
//
//						grammarCorrectionMap.put(startIndex + "-" + endIndex, correction);
//						System.out.println("Grammar: \t" + startIndex + "\tto\t" + endIndex + ":\t" + correction
//								+ "\t(was " + text.substring(startIndex, endIndex) + ")");
//
//					}

					items.add(new SpellingItem(corpusName, id, text, correctionMap, grammarCorrectionMap));
				}
			}
			System.out.println("number of errors: " + nrOfErrors);
			System.out.println("number of grammar errors: " + nrOfGrammarErrors);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		numberOfTexts = items.size();
	}

	@Override
	public void getNext(JCas jcas) throws IOException, CollectionException {
		SpellingItem item = items.poll();
		getLogger().debug(item);

		jcas.setDocumentLanguage(language);
		jcas.setDocumentText(item.getText());

		DocumentMetaData dmd = DocumentMetaData.create(jcas);
		dmd.setDocumentId(String.valueOf(item.getId()));
		dmd.setDocumentTitle(String.valueOf(item.getId()));
		dmd.setCollectionId(item.getCorpusName());

		Map<String, String> corrections = item.getCorrections();

		for (String element : corrections.keySet()) {
			if (errorDetection) {
				SpellingError spellingError = new SpellingError(jcas);
				String[] range = element.split("-");
				spellingError.setBegin(Integer.parseInt(range[0]));
				spellingError.setEnd(Integer.parseInt(range[1]));
				spellingError.setCorrection(corrections.get(element));
				spellingError.addToIndexes();
			} else {
				ExtendedSpellingAnomaly anomaly = new ExtendedSpellingAnomaly(jcas);
				String[] range = element.split("-");
				anomaly.setBegin(Integer.parseInt(range[0]));
				anomaly.setEnd(Integer.parseInt(range[1]));
				anomaly.setGoldStandardCorrection(corrections.get(element));
				anomaly.addToIndexes();
			}
		}


		if (errorDetection) {
			Map<String, String> grammarCorrections = item.getGrammarCorrections();
			for (String element : grammarCorrections.keySet()) {
				GrammarError grammarError = new GrammarError(jcas);
				String[] range = element.split("-");
				grammarError.setBegin(Integer.parseInt(range[0]));
				grammarError.setEnd(Integer.parseInt(range[1]));
				grammarError.setCorrection(grammarCorrections.get(element));
				grammarError.addToIndexes();
			}
		}

		SpellingText text = new SpellingText(jcas, 0, jcas.getDocumentText().length());
		text.setId(item.getCorpusName() + "_" + item.getId());
		text.addToIndexes();

//		for(ExtendedSpellingAnomaly anomaly : JCasUtil.selectCovered(ExtendedSpellingAnomaly.class, text)){
//			System.out.println("Anomaly: "+anomaly.getCoveredText());
//		}

		currentIndex++;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(currentIndex, numberOfTexts, Progress.ENTITIES) };
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return !items.isEmpty();
	}

}
