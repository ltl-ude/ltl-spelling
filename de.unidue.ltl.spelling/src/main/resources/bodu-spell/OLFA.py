#coding=utf-8
'''
Created on 31.07.2018

@author: ronja
'''


from xmltoken import Token, PossibleError
import xmltoken
import filetoxml

import datetime

#class OLFA_error(PossibleError):
#    
#    def __init__(self):
#        PossibleError.__init__(self, id, category, level, morphconst, range, candidate, phon_ok, substituted_range, substituted)


class OLFA_token(Token):
    """
    Inherits everything from xmltoken.Token and adds a method which adds "olfa" as an attribute to a PossibleError object
    """
        
    # Marie (new method): we do not need the possible errors or OLFA types for spelling correction purposes
    def add_errors(self):
        allerrors = set(self.geterrors())
    
    def add_olfa_cat(self):
        
        #OLFA categories are added both in possible as well as committed errors
        allerrors = set(self.getPossibleErrors() + self.geterrors())
        
        for e in allerrors:
            
            #01-KlGr
            if e.category == "low_up": 
                e.olfa = "01-KlGr"
                
            #02-GrKl    
            elif e.category == "up_low":
                e.olfa = "02-GrKl"
            
            #03-GriW
            elif e.category == "up_low_intern":
                e.olfa = "03-GriW"
                
            #04-GetrZus
            #06-GetrUnslbst
            elif e.category == "merge":
                e.olfa = "04-GetrZus"
                
                for m in range(len(self.morphemes_target)):
                    if len(self.morphemes_target) > 1 :
                        if m > 0 :
                            prev_mor =  self.morphemes_target[m-1]
                            if prev_mor.isinMorpheme(e.range[0]-1) :
                                if not prev_mor.ismorend(e.range[0]-1): #splits within a morpheme
                                    e.olfa = "06-GetrUnslbst"
                                if prev_mor.type in ["INFL", "SFX", "PRFX"]: #splits between stem and affix
                                    e.olfa = "06-GetrUnslbst"
                    else:
                        e.olfa = "06-GetrUnslbst" # monomorphemic words
                        
            #05-ZusGetr
            elif e.category == "split":
                e.olfa = "05-ZusGetr"
                
            #07-EinfDoppl   
            elif e.category in ["Cdouble_decofin", "Cdouble_interV", "Cdouble_beforeC", "Cdouble_final", "repl_das_dass"]:
                e.olfa = "07-EinfDoppl"
             
            #08-DopplEinfK    
            elif e.category in  ["hyp_Cdouble", "repl_dass_das"]:
                e.olfa = "08-DopplEinfK"
                
            #09-EinfMarkL
            elif e.category in ["Vlong_i_ie", "Vlong_i_ih","Vlong_i_ieh", "Vlong_single_double", "Vlong_single_h"]:
                e.olfa = "09-EinfMarkL"
                 
            #10-MarkLEinfL  
            elif e.category in ["Vlong_ih_i", "Vlong_ie_i", "Vlong_ieh_i", "Vlong_double_single",  "Vlong_h_single"]:
                e.olfa = "10-MarkLEinfL"
                
            #11-DopplLKons
            elif e.category in ["ovr_Cdouble_afterC", "ovr_Cdouble_afterVlong"]:
                e.olfa = "11-DopplLKons" 
                
            #12-MarkLK  
            elif e.category == "ovr_Vlong_short":
                e.olfa = "12-MarkLK"
                  
            
            #19-ptk-bdg   
            elif e.category == "final_devoice" and e.substituted in ["p", "t", "k"]:
                e.olfa = "19-ptk-bdg" 
                
            #20-bdg-ptk   
            elif e.category == "hyp_final_devoice" and e.substituted in ["b", "d", "g"]:
                e.olfa = "20-bdg-ptk"   
              
            #27-ch-g 
            elif e.category == "final_ch_g":
                e.olfa = "27-ch-g"
                   
            #28-g-ch
            elif e.category == "hyp_final_g_ch":
                e.olfa = "28-g-ch"
                    
            #29-KonsFehlt
            elif e.category in ["ins_C", "ins_morphboundary"]:
                e.olfa = "29-KonsFehlt"
                
            #30-KonsZugef  
            elif e.category in ["del_C", "del_morphboundary"]:
                e.olfa = "30-KonsZugef"
                  
            #31-VokFehlt
            elif e.category in ["ins_V", "schwa"]:
                e.olfa = "31-VokFehlt"
                    
            #32-VokZugef
            elif e.category in ["del_V", "hyp_schwa"]:
                e.olfa = "32-VokFehlt"
                
                    
            #33-KonsFalsch  
            elif e.category in ["repl_CC", "repl_VC", "voice"]:
                e.olfa = "33-KonsFalsch"
            elif e.category == "form" and e.substituted not in ["a", "o", "u", "ä", "ö", "ü"]:
                e.olfa = "33-KonsFalsch"
              
            #34-VokFalsch    
            elif e.category in ["repl_VV", "repl_CV"]:
                e.olfa = "34-VokFalsch"
                
            #35-Umstell   
            elif e.category in ["switch_CC", "switch_CV", "switch_VC", "switch_VV"] :
                e.olfa = "35-Umstell"
                
                
            #36-Uml    
            elif e.category == "form" and e.substituted  in ["a", "o", "u"]:
                e.olfa = "36-Uml"
                
            #37-Sonst
            else:
                e.olfa = "37-Sonst"
            
            
            ##################################################################
            
            # categories which dpenend  on orig/target characters involved 
            #(e.substituted is the hypothetical orig character of a possible error, so o_char is probably superfluous because all errors are systematical ones)
            # e.olfa is overwritten here
            
            for e in allerrors:
                
                #extract  orig and target characters which correspond to each other
                for a in self.characters_aligned:
                    o = a[0]
                    t = a[1]
                    
                    if o == ["<eps>", "<eps>"] or t == ["<eps>", "<eps>"] : continue
                    
                    o_char =  self.orig[o[0]:o[1]+1].lower()
                    t_char = self.target[t[0]:t[1]+1].lower()
                    
                    if any(t in range(e.range[0], e.range[1]+1) for t in range(t[0],t[1]+1)): #if the current target character is in the error range
            
                        #13-s-ß    
                        if (o_char == "s" or e.substituted == "s") and t_char == "ß":
                            e.olfa = "13-s-ß"
                            
                        #14-ß-s
                        if (o_char == "ß" or e.substituted == "ß")  and t_char == "s":
                            e.olfa = "14-ß-s"
                                
                        #15-ss-ß
                        if (o_char == "ss" or e.substituted == "ss")  and t_char == "ß":
                            e.olfa = "15-ss-ß"
                                
                        #16-ß-ss
                        if (o_char == "ß" or e.substituted == "ß")  and t_char == "ss":
                            e.olfa = "16-ß-ss"
                                
                        #17-e-ae
                        if ("e" in o_char or (e.substituted != None and "e" in e.substituted)) and "ä" in t_char and e.category == "repl_unmarked_marked":
                            e.olfa = "17-e-ae"  
                             
                             
                        #18-ae-e
                        if ("ä" in o_char or (e.substituted != None and "ä" in e.substituted)) and "e" in t_char and e.category == "repl_marked_unmarked":
                            e.olfa = "18-ae-e"  
                         
                        #23-f-v    
                        if (o_char == "f" or e.substituted == "f")  and t_char == "v" and e.category == "repl_unmarked_marked":
                            e.olfa = "23-f-v"
                            
                        #24-v-f
                        if (o_char == "v" or e.substituted == "v") and t_char == "f" and e.category == "repl_marked_unmarked":
                            e.olfa = "24-v-f"
                                
                        #25-w-v
                        if (o_char == "w" or e.substituted == "w") and t_char == "v" and e.category == "repl_unmarked_marked":
                            e.olfa = "25-w-v"    
                            
                        #26-v-w
                        if (o_char == "v" or e.substituted == "v") and t_char == "w" and e.category == "repl_marked_unmarked":
                            e.olfa = "26-v-w" 
            

            

def get_errors_with_olfa(this_csv_file=None, textname=None, bas_file=None, user_bas_file=None, stanford_tag=True, use_context = False ):
    
    print("get learner xml",datetime.datetime.now())
    #filetoxml.plaintolearnerxml(csv_file=this_csv_file, input_type = "list", xml_filename = textname + ".xml",  
    #                            with_n_m_alignment=False, bas_file=bas_file, user_bas_file=user_bas_file, stanford_tag = stanford_tag)
    # Marie: without user bas - do we want to keep it like this?
    tokenString = filetoxml.plaintolearnerxmlstring(csv_file=this_csv_file, input_type = "list", xml_filename = textname + ".xml",  
                                with_n_m_alignment=False, bas_file=bas_file, stanford_tag = stanford_tag)
    print("done", datetime.datetime.now())
    
    print("read xml token list",datetime.datetime.now())
    #toks = xmltoken.learnerxmltotokenlist(textname +".xml")
    #toks = xmltoken.learnerxmlstringtotokenlist(textname +".xml")
    toks = xmltoken.learnerxmlstringtotokenlist(tokenString)
    tok_objs = []
    
    for token in toks[0]: #toks[0] is the list of tokens in xml and toks[1] the file_id
        tok_objs.append(OLFA_token(token))
    print("done",datetime.datetime.now())
        
    print("get errors",datetime.datetime.now())
    for token in tok_objs:
        token.add_errors()
    print("done",datetime.datetime.now())
    
    return tok_objs
        


#tok_objs = get_errors_with_olfa("test/test.txt", "test/test", bas_file="KorpusFinal/litkey_corrected.g2p.tab", 
 #                               user_bas_file="KorpusFinal/user_bas_file.g2p.tab", stanford_tag = False, use_context=False)

#for token in tok_objs:
#    for err in token.getterrors():
#        print(token.orig, token.target, err.category, err.olfa, err.range)