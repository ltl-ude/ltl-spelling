
/* First created by JCasGen Sun Jun 21 19:26:16 CEST 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly_Type;

/** 
 * Updated by JCasGen Sun Jun 21 19:26:16 CEST 2020
 * @generated */
public class ExtendedSpellingAnomaly_Type extends SpellingAnomaly_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ExtendedSpellingAnomaly.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
 
  /** @generated */
  final Feature casFeat_corrected;
  /** @generated */
  final int     casFeatCode_corrected;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getCorrected(int addr) {
        if (featOkTst && casFeat_corrected == null)
      jcas.throwFeatMissing("corrected", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_corrected);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCorrected(int addr, boolean v) {
        if (featOkTst && casFeat_corrected == null)
      jcas.throwFeatMissing("corrected", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_corrected, v);}
    
  
 
  /** @generated */
  final Feature casFeat_misspelledTokenText;
  /** @generated */
  final int     casFeatCode_misspelledTokenText;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getMisspelledTokenText(int addr) {
        if (featOkTst && casFeat_misspelledTokenText == null)
      jcas.throwFeatMissing("misspelledTokenText", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return ll_cas.ll_getStringValue(addr, casFeatCode_misspelledTokenText);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMisspelledTokenText(int addr, String v) {
        if (featOkTst && casFeat_misspelledTokenText == null)
      jcas.throwFeatMissing("misspelledTokenText", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    ll_cas.ll_setStringValue(addr, casFeatCode_misspelledTokenText, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ExtendedSpellingAnomaly_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_corrected = jcas.getRequiredFeatureDE(casType, "corrected", "uima.cas.Boolean", featOkTst);
    casFeatCode_corrected  = (null == casFeat_corrected) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_corrected).getCode();

 
    casFeat_misspelledTokenText = jcas.getRequiredFeatureDE(casType, "misspelledTokenText", "uima.cas.String", featOkTst);
    casFeatCode_misspelledTokenText  = (null == casFeat_misspelledTokenText) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_misspelledTokenText).getCode();

  }
}



    