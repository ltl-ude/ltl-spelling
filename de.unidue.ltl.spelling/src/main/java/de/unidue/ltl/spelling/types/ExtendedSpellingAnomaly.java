package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

public class ExtendedSpellingAnomaly extends SpellingAnomaly {

	public ExtendedSpellingAnomaly(JCas jcas) {
		super(jcas);
	}

	boolean fixed = false;

	public void setFixed(boolean wasFixed) {
		this.fixed = wasFixed;
	}

	public boolean getFixed() {
		return this.fixed;
	}

}
