
/* First created by JCasGen Mon Oct 12 11:23:10 CEST 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Thu Oct 22 15:32:28 CEST 2020
 * @generated */
public class SpellingError_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SpellingError.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unidue.ltl.spelling.types.SpellingError");



  /** @generated */
  final Feature casFeat_correction;
  /** @generated */
  final int     casFeatCode_correction;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCorrection(int addr) {
        if (featOkTst && casFeat_correction == null)
      jcas.throwFeatMissing("correction", "de.unidue.ltl.spelling.types.SpellingError");
    return ll_cas.ll_getStringValue(addr, casFeatCode_correction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCorrection(int addr, String v) {
        if (featOkTst && casFeat_correction == null)
      jcas.throwFeatMissing("correction", "de.unidue.ltl.spelling.types.SpellingError");
    ll_cas.ll_setStringValue(addr, casFeatCode_correction, v);}
    
  
 
  /** @generated */
  final Feature casFeat_errorType;
  /** @generated */
  final int     casFeatCode_errorType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getErrorType(int addr) {
        if (featOkTst && casFeat_errorType == null)
      jcas.throwFeatMissing("errorType", "de.unidue.ltl.spelling.types.SpellingError");
    return ll_cas.ll_getStringValue(addr, casFeatCode_errorType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setErrorType(int addr, String v) {
        if (featOkTst && casFeat_errorType == null)
      jcas.throwFeatMissing("errorType", "de.unidue.ltl.spelling.types.SpellingError");
    ll_cas.ll_setStringValue(addr, casFeatCode_errorType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public SpellingError_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_correction = jcas.getRequiredFeatureDE(casType, "correction", "uima.cas.String", featOkTst);
    casFeatCode_correction  = (null == casFeat_correction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_correction).getCode();

 
    casFeat_errorType = jcas.getRequiredFeatureDE(casType, "errorType", "uima.cas.String", featOkTst);
    casFeatCode_errorType  = (null == casFeat_errorType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_errorType).getCode();

  }
}



    