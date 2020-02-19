package de.unidue.ltl.spelling.types;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

public class CorrectedSpellingAnomaly extends SpellingAnomaly{
	
	boolean fixed = false;
	
	public void setFixed(boolean wasFixed) {
		this.fixed = wasFixed;
	}
	
	public boolean getFixed() {
		return this.fixed;
	}

}
