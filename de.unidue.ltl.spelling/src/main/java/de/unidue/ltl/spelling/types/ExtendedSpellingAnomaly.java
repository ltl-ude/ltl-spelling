
/* First created by JCasGen Thu Mar 12 19:49:47 CET 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

/** 
 * Updated by JCasGen Mon Mar 16 12:25:25 CET 2020
 * XML source: /Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/desc/type/Spelling.xml
 * @generated */
public class ExtendedSpellingAnomaly extends SpellingAnomaly {
	/**
	 * @generated
	 * @ordered
	 */
	@SuppressWarnings("hiding")
	public final static int typeIndexID = JCasRegistry.register(ExtendedSpellingAnomaly.class);
	/**
	 * @generated
	 * @ordered
	 */
	@SuppressWarnings("hiding")
	public final static int type = typeIndexID;

	/**
	 * @generated
	 * @return index of the type
	 */
	@Override
	public int getTypeIndexID() {return typeIndexID;}
 
	/**
	 * Never called. Disable default constructor
	 * 
	 * @generated
	 */
	protected ExtendedSpellingAnomaly() {/* intentionally empty block */}
    
	/**
	 * Internal - constructor used by generator
	 * 
	 * @generated
	 * @param addr low level Feature Structure reference
	 * @param type the type of this Feature Structure
	 */
	public ExtendedSpellingAnomaly(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
	/**
	 * @generated
	 * @param jcas JCas to which this Feature Structure belongs
	 */
	public ExtendedSpellingAnomaly(JCas jcas) {
    super(jcas);
    readObject();   
  } 

	/**
	 * @generated
	 * @param jcas  JCas to which this Feature Structure belongs
	 * @param begin offset to the begin spot in the SofA
	 * @param end   offset to the end spot in the SofA
	 */
	public ExtendedSpellingAnomaly(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

	/**
	 * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc
	 * -->
	 *
	 * @generated modifiable
	 */
	private void readObject() {
		/* default - does nothing empty block */}

	// *--------------*
	// * Feature: corrected

	/**
	 * getter for corrected - gets Indicates whether the anomaly has been replaced
	 * with a suggestion
	 * 
	 * @generated
	 * @return value of the feature
	 */
	public boolean getCorrected() {
    if (ExtendedSpellingAnomaly_Type.featOkTst && ((ExtendedSpellingAnomaly_Type)jcasType).casFeat_corrected == null)
      jcasType.jcas.throwFeatMissing("corrected", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((ExtendedSpellingAnomaly_Type)jcasType).casFeatCode_corrected);}
    
	/**
	 * setter for corrected - sets Indicates whether the anomaly has been replaced
	 * with a suggestion
	 * 
	 * @generated
	 * @param v value to set into the feature
	 */
	public void setCorrected(boolean v) {
    if (ExtendedSpellingAnomaly_Type.featOkTst && ((ExtendedSpellingAnomaly_Type)jcasType).casFeat_corrected == null)
      jcasType.jcas.throwFeatMissing("corrected", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((ExtendedSpellingAnomaly_Type)jcasType).casFeatCode_corrected, v);}    
   
    
  //*--------------*
  //* Feature: misspelledTokenText

  /** getter for misspelledTokenText - gets 
   * @generated
   * @return value of the feature 
   */
  public String getMisspelledTokenText() {
    if (ExtendedSpellingAnomaly_Type.featOkTst && ((ExtendedSpellingAnomaly_Type)jcasType).casFeat_misspelledTokenText == null)
      jcasType.jcas.throwFeatMissing("misspelledTokenText", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ExtendedSpellingAnomaly_Type)jcasType).casFeatCode_misspelledTokenText);}
    
  /** setter for misspelledTokenText - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMisspelledTokenText(String v) {
    if (ExtendedSpellingAnomaly_Type.featOkTst && ((ExtendedSpellingAnomaly_Type)jcasType).casFeat_misspelledTokenText == null)
      jcasType.jcas.throwFeatMissing("misspelledTokenText", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    jcasType.ll_cas.ll_setStringValue(addr, ((ExtendedSpellingAnomaly_Type)jcasType).casFeatCode_misspelledTokenText, v);}    
    	
  @Override
	public String getCoveredText() {
		return this.getMisspelledTokenText();
	}
}
