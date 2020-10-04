
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
 * Updated by JCasGen Fri Oct 02 22:45:17 CEST 2020
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
    
  
 
  /** @generated */
  final Feature casFeat_methodThatGeneratedTheCorrection;
  /** @generated */
  final int     casFeatCode_methodThatGeneratedTheCorrection;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getMethodThatGeneratedTheCorrection(int addr) {
        if (featOkTst && casFeat_methodThatGeneratedTheCorrection == null)
      jcas.throwFeatMissing("methodThatGeneratedTheCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return ll_cas.ll_getStringValue(addr, casFeatCode_methodThatGeneratedTheCorrection);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMethodThatGeneratedTheCorrection(int addr, String v) {
        if (featOkTst && casFeat_methodThatGeneratedTheCorrection == null)
      jcas.throwFeatMissing("methodThatGeneratedTheCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    ll_cas.ll_setStringValue(addr, casFeatCode_methodThatGeneratedTheCorrection, v);}
    
  
 
  /** @generated */
  final Feature casFeat_goldStandardCorrection;
  /** @generated */
  final int     casFeatCode_goldStandardCorrection;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getGoldStandardCorrection(int addr) {
        if (featOkTst && casFeat_goldStandardCorrection == null)
      jcas.throwFeatMissing("goldStandardCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return ll_cas.ll_getStringValue(addr, casFeatCode_goldStandardCorrection);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoldStandardCorrection(int addr, String v) {
        if (featOkTst && casFeat_goldStandardCorrection == null)
      jcas.throwFeatMissing("goldStandardCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    ll_cas.ll_setStringValue(addr, casFeatCode_goldStandardCorrection, v);}
    
  
 
  /** @generated */
  final Feature casFeat_spellingEngineCorrection;
  /** @generated */
  final int     casFeatCode_spellingEngineCorrection;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSpellingEngineCorrection(int addr) {
        if (featOkTst && casFeat_spellingEngineCorrection == null)
      jcas.throwFeatMissing("spellingEngineCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    return ll_cas.ll_getStringValue(addr, casFeatCode_spellingEngineCorrection);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSpellingEngineCorrection(int addr, String v) {
        if (featOkTst && casFeat_spellingEngineCorrection == null)
      jcas.throwFeatMissing("spellingEngineCorrection", "de.unidue.ltl.spelling.types.ExtendedSpellingAnomaly");
    ll_cas.ll_setStringValue(addr, casFeatCode_spellingEngineCorrection, v);}
    
  



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

 
    casFeat_methodThatGeneratedTheCorrection = jcas.getRequiredFeatureDE(casType, "methodThatGeneratedTheCorrection", "uima.cas.String", featOkTst);
    casFeatCode_methodThatGeneratedTheCorrection  = (null == casFeat_methodThatGeneratedTheCorrection) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_methodThatGeneratedTheCorrection).getCode();

 
    casFeat_goldStandardCorrection = jcas.getRequiredFeatureDE(casType, "goldStandardCorrection", "uima.cas.String", featOkTst);
    casFeatCode_goldStandardCorrection  = (null == casFeat_goldStandardCorrection) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goldStandardCorrection).getCode();

 
    casFeat_spellingEngineCorrection = jcas.getRequiredFeatureDE(casType, "spellingEngineCorrection", "uima.cas.String", featOkTst);
    casFeatCode_spellingEngineCorrection  = (null == casFeat_spellingEngineCorrection) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_spellingEngineCorrection).getCode();

  }
}



    