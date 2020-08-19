
/* First created by JCasGen Sun Jun 21 19:26:16 CEST 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Tue Aug 18 12:52:23 CEST 2020
 * @generated */
public class KnownWord_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = KnownWord.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.unidue.ltl.spelling.types.KnownWord");



  /** @generated */
  final Feature casFeat_pathToDictItWasFoundIn;
  /** @generated */
  final int     casFeatCode_pathToDictItWasFoundIn;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPathToDictItWasFoundIn(int addr) {
        if (featOkTst && casFeat_pathToDictItWasFoundIn == null)
      jcas.throwFeatMissing("pathToDictItWasFoundIn", "de.unidue.ltl.spelling.types.KnownWord");
    return ll_cas.ll_getStringValue(addr, casFeatCode_pathToDictItWasFoundIn);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPathToDictItWasFoundIn(int addr, String v) {
        if (featOkTst && casFeat_pathToDictItWasFoundIn == null)
      jcas.throwFeatMissing("pathToDictItWasFoundIn", "de.unidue.ltl.spelling.types.KnownWord");
    ll_cas.ll_setStringValue(addr, casFeatCode_pathToDictItWasFoundIn, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public KnownWord_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_pathToDictItWasFoundIn = jcas.getRequiredFeatureDE(casType, "pathToDictItWasFoundIn", "uima.cas.String", featOkTst);
    casFeatCode_pathToDictItWasFoundIn  = (null == casFeat_pathToDictItWasFoundIn) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pathToDictItWasFoundIn).getCode();

  }
}



    