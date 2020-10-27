package de.unidue.ltl.spelling.preprocessing;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Annotates NamedEntities over all tokens whose POS tags correspond to a
 * certain value.
 */
public class POStoNEAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * POS tag to be considered a NE.
	 */
	public static final String PARAM_NE_POS_TAG = "nePosTag";
	@ConfigurationParameter(name = PARAM_NE_POS_TAG, mandatory = true)
	private String nePosTag;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		for (Token token : JCasUtil.select(aJCas, Token.class)) {

			if (token.getPosValue().equals(nePosTag)) {

				NamedEntity ne = new NamedEntity(aJCas);
				ne.setBegin(token.getBegin());
				ne.setEnd(token.getEnd());
				ne.addToIndexes();
			}
		}
	}
}