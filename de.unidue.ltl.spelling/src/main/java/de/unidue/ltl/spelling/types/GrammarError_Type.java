
/* First created by JCasGen Wed Oct 14 16:47:12 CEST 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Wed Oct 14 16:47:12 CEST 2020
 * @generated */
public class GrammarError_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = GrammarError.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unidue.ltl.spelling.types.GrammarError");
 
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
      jcas.throwFeatMissing("correction", "de.unidue.ltl.spelling.types.GrammarError");
    return ll_cas.ll_getStringValue(addr, casFeatCode_correction);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCorrection(int addr, String v) {
        if (featOkTst && casFeat_correction == null)
      jcas.throwFeatMissing("correction", "de.unidue.ltl.spelling.types.GrammarError");
    ll_cas.ll_setStringValue(addr, casFeatCode_correction, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public GrammarError_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_correction = jcas.getRequiredFeatureDE(casType, "correction", "uima.cas.String", featOkTst);
    casFeatCode_correction  = (null == casFeat_correction) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_correction).getCode();

  }
}



    