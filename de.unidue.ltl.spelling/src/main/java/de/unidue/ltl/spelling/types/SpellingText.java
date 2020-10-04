

/* First created by JCasGen Fri Oct 02 22:45:17 CEST 2020 */
package de.unidue.ltl.spelling.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Fri Oct 02 22:45:17 CEST 2020
 * XML source: /Users/mariebexte/ltl-spelling/de.unidue.ltl.spelling/src/main/resources/desc/type/Spelling.xml
 * @generated */
public class SpellingText extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SpellingText.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected SpellingText() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SpellingText(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SpellingText(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SpellingText(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (SpellingText_Type.featOkTst && ((SpellingText_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.unidue.ltl.spelling.types.SpellingText");
    return jcasType.ll_cas.ll_getStringValue(addr, ((SpellingText_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (SpellingText_Type.featOkTst && ((SpellingText_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.unidue.ltl.spelling.types.SpellingText");
    jcasType.ll_cas.ll_setStringValue(addr, ((SpellingText_Type)jcasType).casFeatCode_id, v);}    
  }

    