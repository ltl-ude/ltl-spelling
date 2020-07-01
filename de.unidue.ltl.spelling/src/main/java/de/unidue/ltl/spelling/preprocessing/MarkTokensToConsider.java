package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unidue.ltl.spelling.constants.SpellingConstants;
import de.unidue.ltl.spelling.types.TokenToConsider;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Marks all tokens that do not have annotations of a type that is to be ignored
 * as TokensToConsider.
 */

@ResourceMetaData(name = "")
@DocumentationResource("")
// Input unclear as user is free to pass any desired types
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" }, outputs = {
		"de.unidue.ltl.spelling.types.TokenToConsider" })

public class MarkTokensToConsider extends JCasAnnotator_ImplBase {

	/**
	 * Takes an array of qualified names; all tokens with an annotation of any of
	 * these types will not be considered for spelling correction.
	 */
	public static final String PARAM_TYPES_TO_IGNORE = "typesToIgnore";
	@ConfigurationParameter(name = PARAM_TYPES_TO_IGNORE, mandatory = true, defaultValue = {
			SpellingConstants.NUMERIC_TYPE, SpellingConstants.PUNCTUATION_TYPE, SpellingConstants.NAMED_ENTITY_TYPE })
	private String[] typesToIgnore;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		boolean ignoreToken = false;
		for (Token token : JCasUtil.select(aJCas, Token.class)) {
			ignoreToken = false;
			// Check if token has annotation of any of the types to be excluded
			for (String type : typesToIgnore) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends Annotation> typeToIgnore = (Class<? extends Annotation>) Class.forName(type);
					if (JCasUtil.contains(aJCas, token, typeToIgnore)) {
						System.out.println("Ignoring:\t" + token.getCoveredText()
								+ "\t because it was annotated with type '" + type + "'.");
						ignoreToken = true;
					}
					// Never thrown as JCasUtil.contains throws an exception first
				} catch (ClassCastException e) {
					getContext().getLogger().log(Level.WARNING, "Type '" + type
							+ "' was passed as a type to be ignored, but it does not extend 'org.apache.uima.jcas.tcas.Annotation'.");
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					getContext().getLogger().log(Level.WARNING,
							"Failed to find type '" + type + "' that was passed as a type to ignore: skipping it.");
					e.printStackTrace();
				}
			}
			// If none of the types matched, mark token as one to consider
			if (!ignoreToken) {
				TokenToConsider consider = new TokenToConsider(aJCas);
				consider.setBegin(token.getBegin());
				consider.setEnd(token.getEnd());
				consider.addToIndexes();
				System.out.println("Considering:\t" + token.getCoveredText());
			}
		}
	}
}