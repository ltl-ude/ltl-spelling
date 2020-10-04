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

	protected int currentIndex;
	Queue<SpellingItem> items;
	int numberOfTexts = -1;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
//		System.out.println("Initialize");

		items = new LinkedList<SpellingItem>();

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();

//			int noOfErrors = 0;
			Document document = builder.parse(new FileInputStream(sourceFile));
			Element rootElement = document.getDocumentElement();
			NodeList nodes = rootElement.getChildNodes();
			String corpusName = rootElement.getAttribute("name");
//			System.out.println("corpus name: " + corpusName);

			for (int i = 0; i < nodes.getLength(); i++) {

				int endIndex = 0;
				Map<String, String> correctionMap = new HashMap<String, String>();

				Node node = nodes.item(i);

				if (node instanceof Element) {
					// a child element to process
					Element child = (Element) node;
					String id = child.getAttribute("id");
//					System.out.println("id: " + id);

					String text = child.getTextContent();
//					System.out.println("text: " + text);

					NodeList errors = child.getElementsByTagName("error");

					for (int e = 0; e < errors.getLength(); e++) {

//						noOfErrors++;
						Node error = errors.item(e);

						// a child element to process
						String misspelling = error.getTextContent();
						String correction = ((Element) error).getAttribute("correct");
//						System.out.println("error:\t" + misspelling);
//						System.out.println("correction:\t" + correction);

						int addToIndex = 0;
						try {
							addToIndex = error.getPreviousSibling().getTextContent().length();
						} catch (NullPointerException n) {
							//If no sibling can be retrieved: error directly at beginning of sentence, addToIndex=0
						}

						endIndex += addToIndex;
						int startIndex = endIndex;
						endIndex += misspelling.length();

						correctionMap.put(startIndex + "-" + endIndex, correction);
//						System.out.println(startIndex + "\tto\t" + endIndex + ":\t" + correction + "\t(was "
//								+ text.substring(startIndex, endIndex) + ")");

					}
					items.add(new SpellingItem(corpusName, id, text, correctionMap));
				}
			}
//			System.out.println("number of errors: " + noOfErrors);
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
		
		for(String element : corrections.keySet()) {
			ExtendedSpellingAnomaly anomaly = new ExtendedSpellingAnomaly(jcas);
			String[] range = element.split("-");
			anomaly.setBegin(Integer.parseInt(range[0]));
			anomaly.setEnd(Integer.parseInt(range[1]));
			anomaly.setGoldStandardCorrection(corrections.get(element));
			anomaly.addToIndexes();
		}
		
		SpellingText text = new SpellingText(jcas, 0, jcas.getDocumentText().length());
		text.setId(item.getCorpusName()+"_"+item.getId());
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
