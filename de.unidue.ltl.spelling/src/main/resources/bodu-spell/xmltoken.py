'''
Created on 13.07.2015

Backbone of spelling error computations: Tokens and all necessary information to compute errors are represented as objects with attributes and methods.

@author: Ronja Laarmann-Quante
'''

import xml.dom.minidom
import xml.etree.ElementTree as ET
import g2p
import re
import itertools
import copy
import Levenshtein
import my_python_levenshtein as mylevenshtein
import my_python_levenshtein
import sys
from filetoxml import writexmlrange

vowels = ["a", "e", "i", "o", "u", "ä", "ö", "ü"]
consonants = ["b","c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x","y", "z", "ß"]

vowel_phons = ["2:", "6", "9", "@", "E", "E:", "I", "O", "OY",  "U", "Y", "a", "a:", "aI", "aU", "e", "e:", "eI", "i", "i:", "o", "o:", "u", "u:", "y", "y:" ]

err_cat_mappings_long_short = { "literal" : "lit",
                                "repl_unmarked_marked" : "rpl_unm_mrk",
                                "repl_marked_unmarked" : "rpl_mrk_unm",
                                "ins_clust" : "ins_clust",
                                "del_clust" : "del_clust",
                                "de_foreign" : "de_foreign",
                                "PG_other" : "PG_other",
                                "sepH" : "sepH",
                                "hyp_sepH" : "hyp_sepH",
                                "schwa" : "schwa",
                                "hyp_schwa" : "hyp_schwa",
                                "vocR" : "vocR",
                                "hyp_vocR" : "hyp_vocR",
                                "Cdouble_decofin" : "CC_decofin",
                                "Cdouble_interV" : "CC_interV",
                                "Cdouble_beforeC" : "CC_befC",
                                "Cdouble_final" : "CC_fin",
                                "hyp_Cdouble" : "hyp_CC",
                                "hyp_Cdouble_form" : "hyp_CC_form",
                                "ovr_Cdouble_afterC" : "ovr_CC_aftC",
                                "ovr_Cdouble_afterVlong" : "ovr_CC_aftVlg",
                                "ovr_Vlong_short" : "ovr_Vlg_shrt",
                                "Vlong_i_ie" : "Vlg_i_ie",
                                "Vlong_i_ih" : "Vlg_i_ih",
                                "Vlong_i_ieh" : "Vlg_i_ieh",
                                "Vlong_ih_i" : "Vlg_ih_i",
                                "Vlong_ih_ie" : "Vlg_ih_ie",
                                "Vlong_ih_ieh" : "Vlg_ih_ieh",
                                "Vlong_ie_i" : "Vlg_ie_i",
                                "Vlong_ie_ih" : "Vlg_ie_ih",
                                "Vlong_ie_ieh" : "Vlg_ie_ieh",
                                "Vlong_ieh_i" : "Vlg_ieh_i",
                                "Vlong_ieh_ie" : "Vlg_ieh_ie",
                                "Vlong_ieh_ih" : "Vlg_ieh_ih",
                                "Vlong_otherI" : "Vlg_otherI",
                                "Vlong_double_single" : "Vlg_VV_V ",
                                "Vlong_single_double" : "Vlg_V_VV",
                                "Vlong_h_single" : "Vlg_Vh_V",
                                "Vlong_single_h" : "Vlg_V_Vh",
                                "Vlong_h_double" : "Vlg_Vh_VV",
                                "Vlong_double_h" : "Vlg_VV_Vh",
                                "SL_other" : "SL_other",
                                "final_devoice" : "final_devc",
                                "hyp_final_devoice" : "hyp_final_devc",
                                "final_ch_g" : "final_ch_g",
                                "hyp_final_g_ch" : "hyp_final_g_ch",
                                "ins_morphboundary" : "ins_morphbdr",
                                "del_morphboundary" : "del_morphbdr",
                                "ins_wordboundary" : "ins_wordbdr",
                                "del_wordboundary" : "del_wordbdr",
                                "MO_other " : "MO_other",
                                "form" : "form",
                                "multigraph" : "multigraph",
                                "voice" : "voice",
                                "diffuse" : "diffuse",
                                "repl_VV" : "rpl_VV",
                                "repl_CV" : "rpl_CV",
                                "repl_CC" : "rpl_CC",
                                "repl_VC" : "rpl_VC",
                                "ins_V" : "ins_V",
                                "ins_C " : "ins_C ",
                                "del_V" : "del_V",
                                "del_C " : "del_C ",
                                "swap_VC" : "swp_VC",
                                "swap_CV" : "swp_CV",
                                "swap_CC" : "swp_CC",
                                "swap_VV" : "swp_VV",
                                "up_low" : "up_low",
                                "up_low_intern" : "up_low_intern",
                                "low_up" : "low_up",
                                "split" : "split",
                                "merge" : "merge",
                                "repl_das_dass" : "rpl_das_dass",
                                "repl_dass_das" : "rpl_dass_das",
                                "ins_hyphen_lb" : "ins_hyph_lb",
                                "ins_hyphen_word" : "ins_hyph_word",
                                "del_hyphen" : "del_hyph",
                                "move_hyphen_lb" : "mov_hyph_lb",
                                "keep_hyphen_lb" : "keep_hyph_lb",
                                "punct" : "punct"}


#onsets = consonants - ["ß", "q"] + ["qu","pf", "sch", "pfl", "tr", "pl", "pr", "kn", "kl", "kr", "dr", "gn", "gl", "gr", "zw", "schw", "schm", "schn", "schl", "schr",
#                                    "fl", "fr", "wr", "str", "spl", "spr"]
#nucleus = vowels + [vowel+"h" for vowel in vowels] + ["ie", "aa", "ee", "oo"] + ["au", "eu", "ei", "ai", "äu"] 
#codas = consonants - ["q", "w"] + ["sch", "ch", "pf", "tz", "scht", "ft", "cht", "st", "sp", "mt", "mp", "nt", "nk", "nd", "ng", "nz", "nsch", "nf", "nch", "ns",
#                                   "lt", "lp", "lk", "ld", "lb", "lg", "lz", "lsch", "lf", "lch", "ls", "lm", "ln",
#                                   "rt", "rp", "rk", "rd", "rb", "rg", "rz", "rsch", "rf", "rch", "rs", "rm", "rn", "rl", "rpf"] + ["rzt", 
#                                    "rft", "rcht", "rst", "rnt", "rlt","rscht", "rnst", "rmst", "rfst"] +  ["mpf", "mst", "nst", "nft" "..."]


#estimates if a word is a foreign word or a German word by analyzing the syllable structure of the word
#input: a token of the class "Token"
#output: True (yes it is a foreign word) or False (no it is a German word)
def is_foreign(tok):
    foreign = False    
    foreign_found = False     
    # one-syllable words are considered German
    if len(tok.syllables_target) == 1:
        foreign = False  
        #print(tok.target, "1")
    
    # two-syllable words with pattern stressed - reduced are considered German words
    elif len(tok.syllables_target) == 2 and  tok.syllables_target[0].type == "stress" and tok.syllables_target[1].type == "red":
        foreign = False
        #print(tok.target, "2")
        
    # all other stress patterns are considered foreign words (if there is only one morpheme)
    elif len(tok.morphemes_target) == 1 and len(tok.syllables_target) == 2 and  not (tok.syllables_target[0].type == "stress" and tok.syllables_target[1].type == "red"):
        foreign = True
        #print(tok.target, "7")
        
    # if there are two morphemes and two syllables, the first syllable must be stressed, otherwise it is a foreign word (see BAS analysis of "allein" as PRFX + PTKVZ)
    elif len(tok.morphemes_target) == 2 and len(tok.syllables_target) == 2 and tok.syllables_target[0].type != "stress" :
        foreign = True
        #print(tok.target, "8")
            
    # if more than 2 syllables: it is considered that only ADJ, ADV, NN and V stems can be foreign, these are extracted and their syllable structure is analyzed
    else:
        for morpheme in tok.morphemes_target:
            if foreign_found == True: break
            if morpheme.type in ["ADJ", "ADV", "NN", "V"]:
                
                #remove sequences which are not analyzed as affixes because the "stem" is not a free morpheme ("verlieren", "plötzlich")
                morpheme_range = morpheme.range
                if morpheme.string.startswith("ver"): morpheme_range[0] += 3
                if morpheme.string.endswith("lich"): morpheme_range[1] -= 4
                if morpheme.string.endswith("ig"): morpheme_range[1] -= 2 #fertig
                
                syllable_collection = []
                for syllable in tok.syllables_target:
                    if syllable.range[0] in range(morpheme_range[0], morpheme_range[1]): # if syllable is part of this morpheme; last character of morpheme is intentionally not included because this often belongs to the next syllable already (Kind-er vs. Kin-der)
                        syllable_collection.append(syllable.type)
                    elif syllable.range[0] == morpheme_range[1] and morpheme_range[1] == morpheme.range[0]: # if the last character of the morpheme is its own syllable (e.g. "Lea")
                        syllable_collection.append(syllable.type)
                        
                if len(syllable_collection) > 2: # stems with more than 2 syllables are considered foreign
                    foreign = True
                    foreign_found = True
                    break
                    #print(tok.target, "3")
                elif len(syllable_collection) == 2 and syllable_collection[1] == "red": #if the stem has 2 syllables, the second one has to be reduced (the first may be stressed or unstressed as in compounds only one syllable is annotated as stressed)
                    foreign = False
                    #print(tok.target, "4")
                elif len(syllable_collection) == 1: # stems with one syllable are considered not foreign
                    foreign = False
                    #print(tok.target, "5")
                else:
                    foreign = True
                    foreign_found = True
                    #print(tok.target, "6")
                            
    #print(tok.target, "foreign:", foreign)
    return foreign

def is_plausible_orig(tok):
       
    vowel_re = "[aeiouäöü]+"
    consonant_re = "[bcdfghjklmnpqrstvwxyzß]"
    
    with open("childlex/childlex_0.17.01_litkey_types_onsets.txt", mode ="r", encoding="utf-8") as onset_file:
        onsets = onset_file.read().splitlines()
        onsets.append("") #onset can be empty
        
    with open("childlex/childlex_0.17.01_litkey_types_codas.txt", mode ="r", encoding="utf-8") as coda_file:
        codas = coda_file.read().splitlines()
        codas.append("") #coda can be empty
        
    nuclei = ["a", "e", "i", "o", "u", "ä", "ö", "ü"] + ["ie", "aa", "ee", "oo"] + ["au", "eu", "ei", "ai", "äu"] 
    
    syllable_evaluations = []
    skips = []
    
    
    # construct syllable of the original word that corresponds to the target syllable
    for s in range(len(tok.syllables_target)):
        syl = tok.syllables_target[s]
        
        syl_orig = ""
        #print(syl.range, syl.string)
        #go through all target and corresponding original characters, check if they belong to the currently regarded syllable and collect the orig characters
        for al in tok.characters_aligned:
            orig = al[0]
            target = al[1]
            #print(al)
            
            if target[0] == '<eps>': #add orig characters without corresponding target characters
                if orig[0] not in skips:
                    syl_orig += tok.orig[orig[0]:orig[1]+1]  
                    skips.append(orig[0]) 
            elif syl.range[0] <= target[0] and syl.range[1] >= target[0] and orig[0] != '<eps>' : #if the current target character is in the currently regarded syllable, add the corresponding original character
                syl_orig += tok.orig[orig[0]:orig[1]+1]
            elif syl.range[1] <= target[1]: break #if end of syllable is reached, go to the next syllable
        
        #print(syl_orig)
        syl_orig = re.sub("_", "", syl_orig)
        syl_orig = re.sub("\|", "", syl_orig) 
        
        if not  any(l.isalpha() for l in syl_orig):
            syllable_evaluations.append(None)
        
        
        if s > 0 and len(tok.syllables_target[s-1].string) > 1  :
            if tok.syllables_target[s-1].string[-1] in nuclei or tok.syllables_target[s-1].string[-2] in nuclei and tok.syllables_target[s-1].string[-1] == "h": #if the previous syllable ends with a vowel or vowel+h and the currecnt orig syllable starts with a doubled consonant, the first consonant is ignored (e.g. "Bessen" for "Besen")
                double = re.compile("("+consonant_re+r")\1|ck|tz")
                if re.match(double, syl_orig.lower()):
                    syl_orig = syl_orig[1:]
        
        
        structure = re.compile("^"+"("+consonant_re+"*)"+"("+vowel_re+")"+"("+consonant_re+"*)"+"$")
        
        m = re.match(structure, syl_orig.lower())
        if m:
            onset = m.group(1)
            nucleus = m.group(2)
            coda = m.group(3)

            if onset in onsets and nucleus in nuclei and coda in codas: #structure is legitimate and all parts of the syllable are valid
                syllable_evaluations.append("true")
            
            else:
                
                if nucleus not in nuclei and nucleus not in  ["oi", "ii", "ou", "öu"]: #structure is legitimate but nucleus is invalid; if it is not "oi"/"ii" etc. then it creates a new syllable (e.g. "Schuole")
                    syllable_evaluations.append("sup")
                
                else:
                    syllable_evaluations.append("false") #structure is legitimate but some part is invalid
        
        else:
            
            if not any(vowel in syl_orig for vowel in nuclei):
                syllable_evaluations.append("miss") #if structure is not legitimate and no vowel in the syllable: missed/incomplete syllable
            
            else:
                syllable_evaluations.append("sup") #if structure is not legitimate: further syllables must have been added
    
    
    return syllable_evaluations   
        
    #print(tok.characters_aligned)

#add computed errors and KOFs to a LearnerXML file
#input: list of token objects and LearnerXML file to which the errors are to be added
def errorxml_from_tokenlist(tokenlist, in_learnerXMLfile):
    with open(in_learnerXMLfile, 'r') as utf8file:
        tree = ET.parse(utf8file) #to be processed correctly on Windows, the file has to be opened first
    root = tree.getroot()
    out = open(in_learnerXMLfile, mode = 'w')
    
    #this is now done in filetoxml 
    #with open("childlex/cd_min0.02.txt", mode="r", encoding="utf-8") as childlex_lexicon:
    #    childlex_lexicon = set(childlex_lexicon.read().splitlines())
    #    childlex_lexicon_lower = set([c.lower() for c in childlex_lexicon])
    
    for tok in tokenlist:
        for token in root.iter("token"):
            if token.get("id") == tok.id:
                
                
                ## add irreg_struct
                if tok.target not in ["\h", ""]:
                    if is_foreign(tok): token.set("irreg_struct", str(is_foreign(tok)).lower()) #only include when True
                
                ## add syl_leg
                syll_eval_list = is_plausible_orig(tok)
                syllables = token.find("syllables_target")
                if syllables != None:
                    for syl in syllables.iter("syll"):
                        if syll_eval_list[0] == None:
                            del syll_eval_list[0]
                        else:
                            syl.set("syl_leg", syll_eval_list[0])
                            del syll_eval_list[0]
                            
                ## add KOFs
                xml_kofs = ET.SubElement(token, "key_orthographic_features")
                tok.keyOrthographicFeatures(errors = False)
                id_counter = 0
                for (kof, kofrange) in tok.kofs:
                    id_counter += 1
                    ET.SubElement(xml_kofs, "kof", {"id":"k"+str(id_counter), "cat":kof, "range": writexmlrange("t", kofrange)})
                
                
                # add errors
                xml_errors = ET.SubElement(token, "errors")
                
                # re-align orig and target (but only if errors were found, otherwise it does not work)
                if tok.geterrors() == []: break
                chars_aligned = token.find("characters_aligned")
                for char_a in chars_aligned.findall("char_a"):
                    chars_aligned.remove(char_a)               
             
                aligned_chars = tok.characters_aligned
                num_aligned = 1
                for alignment in aligned_chars:
                    xml_a_char = ET.SubElement(chars_aligned, "char_a", {"id":"a"+str(num_aligned)})
                    if not alignment[0] == ["<eps>","<eps>"]: 
                        xml_a_char.set("o_range", writexmlrange("o", alignment[0]))
                    if not alignment[1] == ["<eps>","<eps>"]:
                        xml_a_char.set("t_range", writexmlrange("t", alignment[1]))   
                    num_aligned += 1    
                
                
                # add error details
                id_counter = 0
                for error in tok.geterrors():
                    id_counter += 1
                    a = False
                    xml_error = ET.SubElement(xml_errors, "err")
                    xml_error.set("level", error.level)
                    xml_error.set("cat_fine", error.category)
                    xml_error.set("id", "e"+str(id_counter))
                
                
                    #if error.range is already an aligned character like "a5" (as set for PG:del) take this #this is fixed in geterrors and should not occur anymore
                    #if str(type(error.range)) != "<class 'list'>":
                    #    xml_error.set("range", error.range)
                    #    a = True
                    #if a == False:
                    a_range = ""
                    for index in range(len(chars_aligned.findall("char_a"))): # otherwise match range of error in target word to id of aligned characters if applicable (usual case)
                        char_a = chars_aligned.findall("char_a")[index]
                        if not error.category in ["del_C", "del_V", "move_hyphen_lb", "keep_hyphen_lb", "del_hyphen", "ins_hyphen_lb", "split", "merge", "del_wordboundary"]:
                            if not (char_a.get("t_range") == None and error.category == "punct"):
                                if char_a.get("t_range") == writexmlrange("t", error.range):
                                    xml_error.set("range", char_a.get("id"))
                                    a = True
                                else:
                                    #range of aligned characters like a1..a2 (for ier -> ihr)
                                    if char_a.get("t_range") != None:                                    
                                        if readrange("t", char_a.get("t_range"))[0] == error.range[0]:
                                            a_range += char_a.get("id")
                                            
                                            # For 3:2 mappings like sch/ch in PG_other, also include the 1st orig character (here <s>) which would otherwise not be part of the error range
                                            if index > 0:
                                                prev_char_a =  chars_aligned.findall("char_a")[index-1]
                                                if prev_char_a.get("t_range") == None and all(err.range != readrange("o", prev_char_a.get("o_range")) for err in tok.geterrors() if err.category not in ["del_V", "del_C"]): #if the previous target character is empty and the orig character is not part of an error range with category del
                                                    a_range = prev_char_a.get("id")
                                            
                                            
                                            a_range += ".."
                                        if a_range != "" and readrange("t", char_a.get("t_range"))[1] == error.range[1]:
                                            
                                            
                                            # for 3:2 mappings like ieh/ie also include the 3rd orig character (here <h>) which would otherwise not be part of the error range
                                            if index < len(chars_aligned.findall("char_a"))-1:
                                                next_char_a =  chars_aligned.findall("char_a")[index+1]
                                                if next_char_a.get("t_range") == None and all(err.range != readrange("o", next_char_a.get("o_range")) for err in tok.geterrors() if err.category not in ["del_V", "del_C"]): #if the next target character is empty and the orig character is not part of an error range with category del
                                                    
                                                    a_range += next_char_a.get("id")
                                                    a = True
                                                    xml_error.set("range", a_range)
                                                
                                                else:
                                                    a_range += char_a.get("id")
                                                    a = True
                                                    xml_error.set("range", a_range)
                                                    
                                            
                                            else:
                                                a_range += char_a.get("id")
                                                a = True
                                                xml_error.set("range", a_range)
                                                
                                            
                                            if error.category in  ["hyp_sepH", "hyp_schwa", "del_clust", "del_morphboundary"]: # up to here cases of e.g. hyp_sepH as in "trauhen" would have range a4..a6 although it should only have a5, this is fixed here
                                                if not (error.category == "del_clust" and error.substituted =="nk"):
                                                    #print(a_range, error.range)
                                                    new_a_range = "a"+str(readrange("a", a_range)[1])                                     
                                                    xml_error.set("range", new_a_range)
                            
                            
                            
                            else: # punct errors like Dodo's -> Dodos
                                if char_a.get("o_range") == writexmlrange("o", error.range):
                                    xml_error.set("range", char_a.get("id"))
                                    a = True
                                    break

                            
                        else: # category del or merge -> no target character there
                            if char_a.get("o_range") == writexmlrange("o", error.range):
                                xml_error.set("range", char_a.get("id"))
                                a = True

                                 
                    if a == False: 
                        xml_error.set("range",  writexmlrange("t", error.range))# if not applicable use target range as error range (for instance for ie:ih [edit: this is a2..a3 now])
                    
                    xml_error.set("cat_short", error.short_cat)
                    
                    xml_error.set("pronc_ok", error.phon_ok)
                    xml_error.set("morph_const", error.morphconst)
                    
                    tok.keyOrthographicFeatures(errors = True)
                    xml_error.set("cat_kof", error.kof_cat)
                
                 
                ## add exist_orig
                #This is now done in filetoxml
                #exist_orig = tok.orig.lower() in childlex_lexicon_lower
                #if tok.target not in ["\h", ""]:
                #    token.set("exist_orig", str(exist_orig).lower())
                
                
        
                        
        
        def prettify(elem): 
            """Return a pretty-printed XML string for the Element. http://stackoverflow.com/questions/17402323/use-xml-etree-elementtree-to-write-out-nicely-formatted-xml-files
            """
            rough_string = ET.tostring(elem, 'utf-8')
            rough_string = re.sub("\n", "", rough_string.decode("utf-8"))
            rough_string = re.sub("\t", "", rough_string)
            reparsed = xml.dom.minidom.parseString(rough_string)
            return reparsed.toprettyxml(indent="\t")  
          
    print(prettify(root), file=out)

# Marie
def learnerxmlstringtotokenlist(learnerXMLstring):
    #tree = ET.parse(learnerXMLstring)
    #root = tree.getroot()
    root = ET.fromstring(learnerXMLstring)
    tokens = []
    for token in root.findall("token"):
        tokens.append(ET.tostring(token, encoding="utf-8"))
    file_id = root.get("id")
    return (tokens, file_id)
    

def learnerxmltotokenlist(learnerXMLfile):
    with open(learnerXMLfile, 'r') as utf8_file: #to be processed correctly on Windows, the file has to be opened first
        tree = ET.parse(utf8_file)
    root = tree.getroot()
    tokens = []
    for token in root.findall("token"):
        tokens.append(ET.tostring(token, encoding="utf-8"))
    file_id = root.get("id")
    return (tokens, file_id)

def readrange(letter, xmlrange): #e.g. t1..t5 becomes [0,4], t1 becomes  [0,0] (=Indices of target characters)
    if ".." in xmlrange:
        return [int(pos.lstrip(letter))-1 for pos in xmlrange.split("..")]
    else:
        return [int(xmlrange.lstrip(letter))-1, int(xmlrange.lstrip(letter))-1]

#takes a list of start and end indices of a range like [0,4] and returns the equivalent range as appears in xml like p1..p5
def writexmlrange(letter, index_list):
    if index_list[0] == index_list[1]:
        return letter+str(index_list[0]+1)
    else:
        return letter+str(index_list[0]+1)+".."+letter+str(index_list[1]+1)
    

class Syllable:
    def __init__(self, xmlsyllable):
        root = ET.fromstring(xmlsyllable)
        self.range = readrange("t", root.get("range"))
        self.rangexml = root.get("range")
        self.type = root.get("type")
        self.string = None
        
    def isinSyllable(self,char_index):
        if char_index in range(self.range[0],self.range[1]+1):
            return True
        return False

    
    def isinonset(self,target, char_range):
        if char_range[0] in range(self.range[0],self.range[1]+1) and char_range[1] in range(self.range[0],self.range[1]+1) : #if character is in this syllable
            if not target[char_range[0]] in vowels and not char_range[1]+1 == len(target) : #if the character is not itself is a vowel (includes double vowels but excludes "qu") and the character is not at the end of the word
                if any(target[r]   in vowels for r in range(char_range[1]+1,self.range[1]+1)): #if any characters right of the character in question is a vowel
                    return True
        return False
    
    def isincoda(self,target, char_range):
        if char_range[0] in range(self.range[0],self.range[1]+1) and char_range[1] in range(self.range[0],self.range[1]+1) : #if character is in this syllable
            if not target[char_range[0]] in vowels and not char_range[1]+1 == len(target): #if the character is not itself is a vowel (includes double vowels but excludes "qu") and the character is not at the end of the word
                if all(target[r]  not in vowels for r in range(char_range[1]+1,self.range[1]+1)): #if all characters right of the character in question are not vowels
                    return True
        return False
        
    

class Morpheme:
    def __init__(self, xmlmorpheme):
        root = ET.fromstring(xmlmorpheme)
        self.range = readrange("t", root.get("range"))
        self.type = root.get("type")
        self.string = None 
        
    def isinMorpheme(self,char_index):
        if char_index in range(self.range[0],self.range[1]+1):
            return True
        return False  
    
    def ismorend(self, char_index):  
        if self.range[1] == char_index:
            return True
        return False  
    
    def ismorstart(self, char_index):
        if self.range[0] == char_index:
            return True
        return False 
        

class PossibleError:
    def __init__(self, id = None, category=None, level=None, morphconst=None, range=None, candidate = None, phon_ok = None, substituted_range=None, substituted=None):
        self.id = id
        self.category = category
        self.level = level
        self.morphconst = morphconst
        self.range = range
        self.candidate = candidate
        self.phon_ok = phon_ok
        self.substituted_range = substituted_range #index in phoneme-aligned-character list
        self.substituted = substituted # substituted character(s)
        self.kof_cat = None # Key orthographic feature, added via token.KeyOrthographicFeatures(errors = True oder False)
        
    #self.short_cat changes whenever self.category changes; it gives the short version of the error category or the error category itself if there is no short version (e.g. 
    #for error combinations when error category is e.g. "pe1,pe7"
    @property
    def short_cat(self):
        if self.category in err_cat_mappings_long_short:
            return err_cat_mappings_long_short[self.category]
        else:
            return self.category

class Token:    
    
    german_lexicon = None 
    phoneme_dict = None
    not_inflectible = ["ADP", "ADV", "FG", "CARD", "INFL", "ITJ", "KOKOM", "KOMP", "KON", "KOUI", "KOUS", "MFG", "ORD", "PAV", 
                       "PRF", "PRFX", "PTKA", "PTKANT", "PTNEG", "PTKVZ", "PTKZU", "PW", "SFX", "SPELL", "XY"]
    bound_morphemes = ["INFL", "PRFX", "SFX", "FG"]
    
    #xmltoken: xml string of one token
    #contextlist (optional): list of all surrounding tokens with ID, orig and target as tuples [('tok1', 'der', 'der'), ('tok2', 'Hunt', 'Hund), ...] 
    def __init__(self, xmltoken, contextlist = None):
        root = ET.fromstring(xmltoken)
        syllables = [ET.tostring(syll, "utf-8") for syll in root.findall("./syllables_target/syll")]    
        morphemes = [ET.tostring(mor, "utf-8") for mor in root.findall("./morphemes_target/mor")]   
        
        self.id = root.get("id")
        self.target = root.get("target")
        self.metamarks = root.get("metamarks")
        self.pos_stts = root.get("pos_stts")
        self.contextlist = contextlist
        
        #metamarks (? ~) are added to target (e.g. ~springte)
        if self.metamarks != None:
            self.target_whole = self.metamarks + self.target
        else:
            self.target_whole = self.target
        
        self.target_istitle = root.get("target").istitle()
        self.target_isupper = root.get("target").isupper()
        self.syllables_target = [Syllable(syll) for syll in syllables]
        self.morphemes_target = [Morpheme(mor) for mor in morphemes]
        
        self.graphemes_target = [] #e.g. ["f", "r", "ö", "h", "l", "i", "ch"]
        for gra in root.findall("./graphemes_target/gra"):
            if gra.get("type") == None:
                self.graphemes_target.append(self.target[readrange("t", gra.get("range"))[0]]) #single-letter graphemes via target slice
            else:
                self.graphemes_target.append(gra.get("type")) #multi letter graphemes via type
        
        
        self.eol = None #orig character index which is last in a line
        self.eoh = None #orig character index which is last in a headline
        for char_o in root.findall("./characters_orig/char_o"):
            if char_o.get("layout") == "EOL":
                self.eol = readrange("o", char_o.get("id"))
            if char_o.get("layout") == "EOH":
                self.eoh = readrange("o", char_o.get("id"))
        
        self.characters_aligned = []
        ###############################
        for char_a in root.findall("./characters_aligned/char_a"):
            if char_a.get("o_range") == None and char_a.get("t_range") != None:
                self.characters_aligned.append([["<eps>","<eps>"], readrange("t", char_a.get("t_range"))])
            elif char_a.get("o_range") != None and char_a.get("t_range") == None:
                self.characters_aligned.append([readrange("o", char_a.get("o_range")), ["<eps>", "<eps>"]])
            else:
                self.characters_aligned.append([readrange("o", char_a.get("o_range")), readrange("t", char_a.get("t_range"))])
        ################################
        
        self.new_aligned = None
        
        self.target_phonemes = None
        
        self.grapheme_phoneme_alignment = []
        self.grapheme_phoneme_alignment_indices = []
        ###############################
        #for phon_a in root.findall("./phonemes_aligned/phon_a"):
        #    t_range = phon_a.get("t_range")
        #    p_range = phon_a.get("p_range")
        #    
        #    if t_range == None and p_range != None:
        #        self.grapheme_phoneme_alignment.append(("<eps>", root.find(".//*[@id='"+p_range+"']").text)) 
        #        
        #    elif t_range != None and p_range == None:
        #        target_slice = ""
        #        for index in (range(readrange("t", t_range)[0], readrange("t", t_range)[-1]+1)):
        #            target_slice += self.target[index]
        #        self.grapheme_phoneme_alignment.append((target_slice, "<eps>"))
        #    
        #    elif t_range != None and p_range != None:
        #        target_slice = ""
        #        for index in (range(readrange("t", t_range)[0], readrange("t", t_range)[-1]+1)):
        #            target_slice += self.target[index]
        #        self.grapheme_phoneme_alignment.append((target_slice, root.find(".//*[@id='"+p_range+"']").text))
        
        trange_counter = 0
        
        for phon_t in root.findall("./phonemes_target/phon_t"):
            t_range = phon_t.get("t_range")
            #p_range = phon_a.get("p_range")
            
            if t_range == None:
                self.grapheme_phoneme_alignment.append(("<eps>",phon_t.text))# root.find(".//*[@id='"+p_range+"']").text)) 
                           
            elif t_range != None:
                
                # target characters without corresponding phonemes (e.g. "h" in "sehen")
                target_slice = ""
                while readrange("t", t_range)[0] - trange_counter  > 1: 
                    target_slice += root.find(".//*[@id='"+"t"+str(trange_counter+1+1)+"']").text
                    self.grapheme_phoneme_alignment.append((target_slice,"<eps>"))
                    trange_counter += 1
                
                # target characters with corresponding phonemes
                target_slice = ""
                for index in (range(readrange("t", t_range)[0], readrange("t", t_range)[-1]+1)):
                    target_slice += self.target[index]
                self.grapheme_phoneme_alignment.append((target_slice, phon_t.text))
                trange_counter = readrange("t", t_range)[1]
        
        # target characters without corresponding phonemes at the end of the word (e.g. "h" in "seh")
        if trange_counter < len(self.target)-1: 
            for s in range(trange_counter+1, len(self.target)):
                self.grapheme_phoneme_alignment.append((self.target[s], "<eps>"))
        
        #print(self.grapheme_phoneme_alignment)        
        
        start_char = 0
        for i in range(len(self.grapheme_phoneme_alignment)):
            char = self.grapheme_phoneme_alignment[i][0]
            if char == "<eps>":
                self.grapheme_phoneme_alignment_indices.append([-1, -1]) #Empty String = -1
                continue
            self.grapheme_phoneme_alignment_indices.append([start_char,start_char+len(char)-1])
            start_char += len(char)
        ###################################
        
        self.orig = root.get("orig")
        self.errors = None
        
        #self.phonetic_dictionary_file = phonetic_dictionary_file
        #if Token.phoneme_dict == None and self.phonetic_dictionary_file != None:
        #    phonemes = open(self.phonetic_dictionary_file, mode="r", encoding="utf-8").read().splitlines()
        #    phoneme_dict = {"".join(line.split(";")[0].split()) : "".join(line.split(";")[1].split()) for line in phonemes}
        #    Token.phoneme_dict = phoneme_dict
        
        self.possErrors = None
        self.possErrorCombinations = None
        self.kofs = None #list of tuples [(kof, target_range), ...]
        self.kof_dict = None
        self.kof_error_dict = None

        for syll in self.syllables_target:
            syll.string = self.target[syll.range[0]:syll.range[1]+1]
            
        for mor in self.morphemes_target:
            mor.string = self.target[mor.range[0]:mor.range[1]+1]
    
    # Marie: to simplify output
    def get_target(self):
        return self.target
            
    def morph_const_bound_morphemes(self, char_start_index, list_of_morph_tags_to_include):
        for mor in self.morphemes_target:
            if mor.isinMorpheme(char_start_index):
                if mor.type  in list_of_morph_tags_to_include:
                    return True
        return False
        
    def morph_const(self, char_start_index, char_end_index, list_of_morph_tags_to_exclude):
        for mor in self.morphemes_target:
            if mor.isinMorpheme(char_start_index):
                if mor.ismorend(char_end_index):
                    if mor.type not in list_of_morph_tags_to_exclude:
                        return True
        return False
         
    
    
    #returns a list of aligned grapheme, phoneme tuples
    def getAlignedTargetPhonemes(self):
        return self.grapheme_phoneme_alignment

    
    def getAlignedTargetPhonemeIndices(self):
        return self.grapheme_phoneme_alignment_indices

    
    # if an unsystematic error occurs within a single PCU this should be treated as one error, e.g. "wal" for "weil" and "a" should be aligned with "ei"
    # alignment with levenshtein would result in [[[0, 0], [0, 0]], [['<eps>', '<eps>'], [1, 1]], [[1, 1], [2, 2]], [[2, 2], [3, 3]]], hence this would be regarded as 2 errors
    # this function changes [['<eps>', '<eps>'], [1, 1]], [[1, 1], [2, 2]] to [[1, 1], [1, 2]] because [1, 2] is a PCU ("ei")
    # only works with PCUs of length 2
    def fix_diphthong_alignment(self, characters_aligned):
        with_diphthongs_aligned = []
        with_diphthongs = False
        skip = False

        for i in range(len(characters_aligned)):
            if  skip == True:
                with_diphthongs = False
                skip = False
                continue
            
            with_diphthongs = False
            orig = characters_aligned[i][0]
            target = characters_aligned[i][1]
            if orig == ["<eps>", "<eps>"]: # if there was a deletion in the original
                for j in range(len(self.getAlignedTargetPhonemeIndices())):
                    phonemes = self.getAlignedTargetPhonemeIndices()[j]
                    if phonemes[1] - phonemes[0] > 0 and target[0] in phonemes: #if there is a PCU > 1 character and the target character belongs to them
                        for k in range(len(characters_aligned)): # search for the rest part of this PCU and join them
                            if characters_aligned[k][1][1] in phonemes:
                                new_unit = []
                                if k - i == 1: 
                                    new_unit.append(characters_aligned[k][0])
                                    new_unit.append([characters_aligned[i][1][0], characters_aligned[k][1][1]])           
                                    with_diphthongs_aligned.append(new_unit)
                                    with_diphthongs = True
                                    skip = True
                                elif i - k == 1:
                                    new_unit.append(characters_aligned[k][0])
                                    new_unit.append([characters_aligned[k][1][0], characters_aligned[i][1][1]])           
                                    with_diphthongs_aligned[-1] = new_unit
                                    with_diphthongs = True
                                    skip = False
            
            if with_diphthongs == False:
                with_diphthongs_aligned.append(characters_aligned[i])
                
        #print("with_diph", with_diphthongs_aligned)
        return with_diphthongs_aligned
    
  
    def getPossibleErrors(self):
        
        #return list if it was already computed before
        if self.possErrors != None:
            return self.possErrors
        
        #ignore punctuation marks, numbers etc.
        if not re.search("[abcdefghijklmnopqrstuvwxyzäöüß]", self.target.lower()):
            return []
        
        # otherwise start computation of errors
        possibleErrors = []
        err_id = 1
        
        # lowercase target word (temporarily)           
        #self.target = self.target.lower()
        
        # set variables for ,quick reference
        charphon = self.getAlignedTargetPhonemes()
        charphon = [(c.lower(), p) for (c,p) in charphon] #lower all characters
        char_indices = self.getAlignedTargetPhonemeIndices()

        for i in range(len(charphon)):
        
          
            char = charphon[i][0]
            phon = charphon[i][1]
            chars_index = char_indices[i] #list like [4,5]
            chars_start_index = char_indices[i][0] # 4 in above example
            chars_end_index = char_indices[i][1] # 5 in above example
            
            
            if i < len(charphon)-1:
                next_phon = charphon[i+1][1]
                next_char = charphon[i+1][0]
                next_chars_index = char_indices[i+1]
                next_chars_start_index = char_indices[i+1][0]
                next_chars_end_index = char_indices[i+1][1]
                
            if i > 0:
                prev_phon = charphon[i-1][1]
                prev_char = charphon[i-1][0]
                prev_charS_index = char_indices[i-1]
                prev_chars_start_index = char_indices[i-1][0]
                prev_chars_end_index = char_indices[i-1][1]
                
        
            # "a" occurs only in unstressed syllables and can be tense or lax  -> in open syllable it must be tense
            a_in_open_syllable = False
            for syl in self.syllables_target:
                if syl.isinSyllable(chars_start_index):
                    if syl.string.endswith("a"):
                        a_in_open_syllable = True
            
            ### literal
            if i < len(charphon)-1:
                if char == "s" and phon == "S":     
                    if next_char == "p" or next_char == "t": # sp -> schp, st -> scht
                        candidate = list(self.target)
                        candidate[chars_start_index] += "ch"
                        candidate = "".join(candidate)
                        e = PossibleError(category = "literal", level="PGI", morphconst="na", range=[chars_start_index, chars_end_index], candidate = candidate, phon_ok = "true", substituted_range=i, substituted = candidate[chars_start_index]+"ch", id = err_id)   
                        possibleErrors.append(e)
                        err_id += 1 
                        
            if char == "äu" or char == "eu":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "oj"
                candidate = "".join(candidate)
                e = PossibleError(category = "literal", level="PGI", morphconst="na", range=[chars_start_index, chars_end_index], candidate = candidate, phon_ok = "true", substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)   
                possibleErrors.append(e)
                err_id += 1 
               
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "oi"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "oi"
                e1.id = err_id
                possibleErrors.append(e1)
                err_id += 1 
            
            if char == "au":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ao"
                candidate = "".join(candidate)
                e = PossibleError(category = "literal", level="PGI", morphconst="na", range=[chars_start_index, chars_end_index], candidate = candidate, phon_ok = "true", substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)   
                possibleErrors.append(e)
                err_id += 1 
                    
            
            if char == "ei" or char == "ai":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "aj"
                candidate = "".join(candidate)
                e = PossibleError(category = "literal", level="PGI", morphconst="na", range=[chars_start_index, chars_end_index], candidate = candidate, phon_ok = "true", substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)   
                possibleErrors.append(e)
                err_id += 1 
                
                
            if char == "qu":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "kw"  
                candidate = "".join(candidate)
                e = PossibleError(category = "literal", level="PGI", morphconst="na", range=[chars_start_index, chars_end_index], candidate = candidate,  phon_ok = "true", substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)   
                possibleErrors.append(e)
                err_id += 1 

            
            
            ### repl_marked_unmarked
            candidate = list(self.target)
            
            if  char == "ei":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ai"
                e.candidate = "".join(candidate)
                e.substituted = "ai"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "eu":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "äu"
                e.candidate = "".join(candidate)
                e.substituted = "äu"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char[0] == "e":# and phon == "E":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                if len(char) == 1:
                    candidate[chars_start_index:chars_end_index+1] = "ä"
                    e.substituted = "ä"
                    
                elif len(char) == 2: #eh
                    candidate[chars_start_index:chars_start_index+1] = "ä"
                    e.substituted = "ä"+char[1]
                    
                e.candidate = "".join(candidate)
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char[0] == "ü":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                if len(char) == 1:
                    candidate[chars_start_index:chars_end_index] = "y"
                    e.substituted = "y"
                elif len(char) == 2: # üh
                    candidate[chars_start_index:chars_start_index+1] = "y"
                    e.substituted = "y"+char[1]
                
                e.candidate = "".join(candidate)
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "j":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "y"
                e.candidate = "".join(candidate)
                e.substituted = "y"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "k":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ch"
                e.candidate = "".join(candidate)
                e.substituted = "ch"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "ch" and phon == "k":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "c"
                e.candidate = "".join(candidate)
                e.substituted = "c"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            if char == "k":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "c"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "c"
                e1.id = err_id
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e1)
                err_id += 1 
                
            elif char == "x" and phon == "ks":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "chs"
                e.candidate = "".join(candidate)
                e.substituted = "chs"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "x" and phon == "ks":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ks"
                e.candidate = "".join(candidate)
                e.substituted = "ks"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "t":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "dt"
                e.candidate = "".join(candidate)
                e.substituted = "dt"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            if char == "t":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "th"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "th"
                e1.id = err_id
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e1)
                err_id += 1 
                
                
            elif char == "w":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "v"
                e.candidate = "".join(candidate)
                e.substituted = "v"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "f":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "v"
                e.candidate = "".join(candidate)
                e.substituted = "v"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            if char == "f":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ph"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "ph"
                e1.id = err_id
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e1)
                err_id += 1 
                
                
            elif char == "v" and phon == "f":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ph"
                e.candidate = "".join(candidate)
                e.substituted = "ph"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "z" and phon == "ts":
                e = PossibleError(category = "repl_marked_unmarked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ts"
                e.candidate = "".join(candidate)
                e.substituted = "ts"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  

                
            
            ### repl_unmarked_marked
            
            # heuristic for morphconst for words with <ä> --> exceptions have to be added
            def ae_morphconst_neces(word):
                    with open("childlex/childlex_0.17.01_litkey_types.txt", mode="r", encoding="utf-8") as childlex_lexicon:
                        childlex_lexicon = set(childlex_lexicon.read().splitlines())
                        childlex_lexicon_lower = set([c.lower() for c in childlex_lexicon])
                    changed = re.sub("ä", "a", word)
                    changed = changed.lower()
                    if changed in childlex_lexicon_lower: return True # Äpfel - Apfel
                    elif changed.rstrip("e") in childlex_lexicon_lower: return True # Mäuse - Maus
                    elif changed.rstrip("er") in childlex_lexicon_lower: return True # Häuser - Haus
                    elif changed.rstrip("en") in childlex_lexicon_lower: return True 
                    elif changed.rstrip("ern") in childlex_lexicon_lower: return True
                    return False
            
            
            candidate = list(self.target)
            
            if  char == "ai":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ei"
                e.candidate = "".join(candidate)
                e.substituted = "ei"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "äu":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "eu"
                e.candidate = "".join(candidate)
                e.substituted = "eu"
                # heuristic for morphconst
                if ae_morphconst_neces(self.target):
                    e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char[0] == "ä": #and phon == "E":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                if len(char) == 1:
                    candidate[chars_start_index:chars_end_index+1] = "e"
                    e.candidate = "".join(candidate)
                    e.substituted = "e"
                elif len(char) == 2: # äh
                    candidate[chars_start_index:chars_start_index+1] = "e"
                    e.candidate = "".join(candidate)
                    e.substituted = "e"+char[1]
                # heuristic for morphconst
                if ae_morphconst_neces(self.target):
                    e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "y" and re.search("y", phon):
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ü"
                e.candidate = "".join(candidate)
                e.substituted = "ü"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "y" and re.search("i", phon):
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "i"
                e.candidate = "".join(candidate)
                e.substituted = "i"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1
                
            elif char == "y" and phon == "j":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "j"
                e.candidate = "".join(candidate)
                e.substituted = "j"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "ch" and phon == "k":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "k"
                e.candidate = "".join(candidate)
                e.substituted = "k"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            elif char == "c" and phon == "k":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "ch"
                e.candidate = "".join(candidate)
                e.substituted = "ch"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
            if char == "c"  and phon == "k":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "k"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "k"
                e1.id = err_id
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e1)
                err_id += 1 
                
            
            elif char == "dt":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "t"
                e.candidate = "".join(candidate)
                e.substituted = "t"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "th":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "t"
                e.candidate = "".join(candidate)
                e.substituted = "t"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1           
                
                
            elif char == "v" and phon == "v":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "w"
                e.candidate = "".join(candidate)
                e.substituted = "w"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "v" and phon == "f":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "f"
                e.candidate = "".join(candidate)
                e.substituted = "f"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            elif char == "ph" :
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate[chars_start_index:chars_end_index+1] = "f"
                e.candidate = "".join(candidate)
                e.substituted = "f"
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e)
                err_id += 1  
                
                
            if char == "ph":
                e = PossibleError(category = "repl_unmarked_marked", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "v"
                e1 = copy.deepcopy(e)
                e1.candidate = "".join(candidate)
                e1.substituted = "v"
                e1.id = err_id
                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                possibleErrors.append(e1)
                err_id += 1 
            
            
            
            ### ins_clust
            found = False
            if i > 0 and i < len(charphon)-1:
                if char in ["b", "d", "g", "p", "t", "k"] and charphon[i-1][0][-1] in consonants and charphon[i-1][0] != "r" and next_char[0] in consonants:
                    
                    #must all be in same morpheme or in stem + affix morphemes (not multiple stems as in "achtunddreißig")
                    for m in range(len(self.morphemes_target)):
                        morpheme = self.morphemes_target[m]
                        if all(morpheme.isinMorpheme(ch) for ch in [chars_start_index, prev_chars_start_index, next_chars_start_index]):
                            found = True 
                            
                        else:
                            if m > 0 and morpheme.isinMorpheme(chars_start_index) and morpheme.ismorstart(chars_start_index):
                                if morpheme.type in Token.bound_morphemes or self.morphemes_target[m-1] in Token.bound_morphemes:
                                    found = True
                                    
                            if m < len(self.morphemes_target)-1 and morpheme.isinMorpheme(chars_start_index) and morpheme.ismorend(chars_end_index):
                                if morpheme.type in Token.bound_morphemes or self.morphemes_target[m+1] in Token.bound_morphemes:
                                    found = True
                    
                    
                    if found == True:
                        candidate = list(self.target)
                        del candidate[chars_start_index]
                        candidate = "".join(candidate)
                        e = PossibleError(category = "ins_clust", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                        
                        for morpheme in self.morphemes_target:
                            if  morpheme.ismorend(chars_end_index): 
                                e.morphconst="neces"
                        if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e.morphconst = "na"
                                    
                        possibleErrors.append(e)
                        err_id += 1 

            
                #ferd -> pferd, Bratfanne -> Bratpfanne
            if i < len(charphon)-1 and found == False:
                for morpheme in self.morphemes_target:
                    if morpheme.ismorstart(chars_start_index):
                        if char == "p" and next_char == "f":
                            candidate = list(self.target)
                            del candidate[chars_start_index]
                            candidate = "".join(candidate)
                            e = PossibleError(category = "ins_clust", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                            
                            possibleErrors.append(e)
                            err_id += 1 
                
                
            ### del_clust
            
                # Bsp. Hemd, singst, rennst etc. bei Hall (2011, 2000) finden!
            if i < len(charphon)-1:
                if phon == "m" and next_phon == "t" or phon in  ["l", "n"] and next_phon == "s" or phon == "N" and next_phon in  ["s", "t"]:
                    for syl in self.syllables_target:
                        if syl.isinSyllable(chars_start_index) and syl.isinSyllable(next_chars_start_index): # have to be in same syllable
                            
                            candidate = list(self.target)
                            if phon == "m":
                                candidate[chars_start_index:chars_end_index+1] += "p"
                                subst = "".join(candidate[chars_start_index: chars_end_index+1]) +"p"
                            elif phon in ["l", "n"]:
                                candidate[chars_start_index:chars_end_index+1] += "t"
                                subst = "".join(candidate[chars_start_index: chars_end_index+1]) +"t"
                            elif phon == "N":
                                candidate[chars_start_index:chars_end_index+1] += "k"
                                subst = "".join(candidate[chars_start_index: chars_end_index+1]) +"k"
                            candidate = "".join(candidate)
                            
                            e = PossibleError(category = "del_clust", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_end_index, next_chars_start_index], candidate = candidate, substituted_range=i, substituted = subst, id = err_id)
                            possibleErrors.append(e)
                            err_id += 1 
                            
                            
                            candidate = list(self.target)
                            if phon == "m":
                                candidate[chars_start_index:chars_end_index+1] += "b"
                                subst = "".join(candidate[chars_start_index: chars_end_index+1]) +"b"
                            elif phon in ["l", "n"]:
                                candidate[chars_start_index:chars_end_index+1] += "d"
                                subst = "".join(candidate[chars_start_index: chars_end_index+1]) +"d"
                            elif phon == "N":
                                candidate[chars_start_index:chars_end_index+1] = "nk"
                                subst = "nk"
                            
                            e1 = copy.deepcopy(e)
                            e1.candidate = "".join(candidate)
                            e1.substituted = subst
                            e1.id = err_id
                            
                            if subst == "nk": e1.range=[chars_start_index,chars_end_index]
                            
                            possibleErrors.append(e1)
                            err_id += 1 
                            
                            
            
               
                #pfahren -> fahren
            if i < len(charphon)-1 and found == False:
                for morpheme in self.morphemes_target:
                    if morpheme.ismorstart(chars_start_index):
                        if char == "f":
                            candidate = list(self.target)
                            candidate[chars_start_index:chars_end_index+1] = "pf"
                            candidate = "".join(candidate)
                            
                            e = PossibleError(category = "del_clust", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "pf", id = err_id)
                            possibleErrors.append(e)
                            err_id += 1 
            
            
            ### de_foreign    
            if char == "n" and phon == "N":
                e = PossibleError(category = "de_foreign", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ng"
                e.candidate = "".join(candidate)
                e.substituted="ng"
                possibleErrors.append(e)
                err_id += 1
                
            if char == "ay":
                e = PossibleError(category = "de_foreign", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ej"
                e.candidate = "".join(candidate)
                e.substituted="ej"
                possibleErrors.append(e)
                err_id += 1 
                
            if char == "a" and phon == "E":
                e = PossibleError(category = "de_foreign", level="PGI", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "e"
                e.candidate = "".join(candidate)
                e.substituted="e"
                possibleErrors.append(e)
                err_id += 1   
                
                e1 = copy.deepcopy(e)
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ä"
                e1.candidate = "".join(candidate)
                e1.substituted="ä"
                possibleErrors.append(e1)
                err_id += 1 
            
            
            ### PG_other
               
                # Schina/China, isch/ich
            if char == "ch" and phon == "C":
                e = PossibleError(category = "PG_other", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "sch"
                e.candidate = "".join(candidate)
                e.substituted="sch"
                possibleErrors.append(e)
                err_id += 1

                
                #chemand/jemand
            if char == "j" and phon == "j":
                e = PossibleError(category = "PG_other", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ch"
                e.candidate = "".join(candidate)
                e.substituted="ch"
                possibleErrors.append(e)
                err_id += 1
                
                #zaygte/zeigte
            if char == "ei":
                e = PossibleError(category = "PG_other", level="PGI", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)   
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ay"
                e.candidate = "".join(candidate)
                e.substituted="ay"
                possibleErrors.append(e)
                err_id += 1
                
                #dabeiy/dabei
                e1 = copy.deepcopy(e)
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "eiy"
                e1.candidate = "".join(candidate)
                e1.substituted="eiy"
                e1.id = err_id
                possibleErrors.append(e1)
                err_id += 1
             
            
            
            ### vocR
            if phon == "6":
                if  i > 0 and prev_phon in ["a", "a:"]: # if the vowel before is already an /a/ as in "sogar" the <r> is deleted
                    candidate = list(self.target)
                    del candidate[chars_start_index]
                    candidate = "".join(candidate)
                    e = PossibleError(category = "vocR", level="SL", morphconst="na",  phon_ok = "true", range=[prev_chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                    
                    
                    #end of inflecting morpheme
                    for morpheme in self.morphemes_target:
                        if  morpheme.ismorend(chars_end_index): 
                            e.morphconst="neces"
                    if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e.morphconst = "na"
                    
                    #bound morpheme
                    if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                    
                    possibleErrors.append(e)
                    err_id += 1 
                    
                else:    
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "a"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "vocR", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "a", id = err_id)
                    
                    if i > 0 and prev_phon in vowel_phons:
                        e.range = [prev_chars_start_index, chars_end_index]
                    
                    #end of inflecting morpheme
                    for morpheme in self.morphemes_target:
                        if  morpheme.ismorend(chars_end_index): 
                            e.morphconst="neces"
                    if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e.morphconst = "na"
                    
                    #bound morpheme
                    if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                         
                    possibleErrors.append(e)
                    err_id += 1
            
            if phon == "r": #see transcription of "wahr"
                for syllable in self.syllables_target:
                    #if r is in the coda of any syllable or onset of a reduced syllable with  "en" (see "garen" -> "gan" [probably mostly useful together with schwa then])
                    if  syllable.isinSyllable(chars_start_index) and syllable.isincoda(self.target, chars_index) or syllable.isinSyllable(chars_start_index)  and syllable.type == "red" and syllable.string.endswith("en") :
                        if  i > 0 and prev_phon in  ["a", "a:"] : # if the vowel before is already an /a/ as in "Lars" the <r> is deleted
                            candidate = list(self.target)
                            del candidate[chars_start_index]
                            candidate = "".join(candidate)
                            e = PossibleError(category = "vocR", level="SL", morphconst="na",  phon_ok = "true", range=[prev_chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                            if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible): e.morphconst = "neces"
                            
                            #bound morpheme
                            if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                            
                            # Oan -> Ohren = colloquial pronunciation
                            if syllable.isinSyllable(chars_start_index) and syllable.type == "red" and syllable.string.endswith("en"):
                                e.phon_ok = "coll"
                                        
                            possibleErrors.append(e)
                            err_id += 1 
                            
                        else:    
                            candidate = list(self.target)
                            candidate[chars_start_index: chars_end_index+1] = "a"
                            candidate = "".join(candidate)
                            e = PossibleError(category = "vocR", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "a", id = err_id)
                            
                            if i > 0 and prev_phon in vowel_phons:
                                e.range = [prev_chars_start_index, chars_end_index]
                            
                            for morpheme in self.morphemes_target:
                                    if  morpheme.ismorend(chars_end_index): 
                                        e.morphconst="neces"
                            if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e.morphconst = "na"
                            
                            #bound morpheme
                            if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                            
                            # Fahn -> Fahren = colloquial pronunciation
                            if syllable.isinSyllable(chars_start_index) and syllable.type == "red" and syllable.string.endswith("en"):
                                e.phon_ok = "coll"
                                  
                            possibleErrors.append(e)
                            err_id += 1
                        
            
            ### hyp_vocR
            if phon == "a:" or phon == "a":
                if  i < len(charphon)-1 and not next_char == "r" or i == len(charphon)-1: # applies after each /a/ except if an <r> is already present
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] += "r"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "hyp_vocR", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index: chars_end_index+1] + "r", id = err_id)
                    
                    if phon == "a" and a_in_open_syllable == False:
                        e.phon_ok = "false"
                    
                    for morpheme in self.morphemes_target:
                            if morpheme.ismorend(chars_end_index):
                                e.morphconst="neces"
                    if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e.morphconst = "na"
                    
                    #bound morpheme
                    if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                               
                    possibleErrors.append(e)
                    err_id += 1
                    
                    if phon == "a":
                        for syllable in self.syllables_target:
                            if syllable.isinSyllable(chars_start_index) and syllable.type != "stress": #er as candidate only in unstressed syllables
                                candidate = list(self.target)
                                candidate[chars_start_index:chars_end_index+1] = "er"
                                e1 = copy.deepcopy(e)
                                e1.candidate = "".join(candidate)
                                e1.substituted = "er"
                                e1.id = err_id
                                
                                if a_in_open_syllable == False:
                                    e1.phon_ok = "false"
                                        
                                
                                for morpheme in self.morphemes_target:
                                    if  morpheme.ismorend(chars_end_index): 
                                        e1.morphconst="neces"
                                if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False: e1.morphconst = "na"
                                    
                                #bound morpheme
                                if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e1.morphconst = "neces"
                
                                possibleErrors.append(e1)
                                err_id += 1 
                
            
            ### schwa
            if i < len(charphon)-1 and  phon == "@":
                if next_phon in ["l", "m", "n"]:
                    for s in range(len(self.syllables_target)):
                        syl = self.syllables_target[s]
                        if syl.isinSyllable(chars_start_index) and syl.isinSyllable(next_chars_start_index): # Schwa and l,m,n have to be in the same syllable ("lesen" matches but "gemacht" not)
                            
                            candidate = list(self.target)
                            del candidate[chars_start_index]
                            candidate = "".join(candidate)
                            e = PossibleError(category = "schwa", level="SL", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                            if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                            
                            for mor in self.morphemes_target:
                                if mor.isinMorpheme(chars_start_index):
                            
                                    #"official" positions according to pronunciation Duden: hrere phon_orig_ok is "true" instead of coll
                                    if (i > 0 and ( next_phon == "l" and charphon[i-1][1] in ["n", "m", "N", "S", "Z", "b", "d", "g", "p", "t", "k", "f", "v", "s", "z", "ts", "x", "C"]) 
                                        or (next_phon == "m" and charphon[i-1][1] in ["S", "Z","f", "v", "s", "z", "ts", "x", "C"])
                                        or (next_phon == "n" and charphon[i-1][1] in ["S", "Z", "b", "d", "g", "p", "t", "k", "f", "v", "s", "z", "ts", "x", "C"] and mor.string != "chen")):
                                            e.phon_ok = "true"
                                
                            
                                
                                
                            possibleErrors.append(e)
                            err_id += 1
        
            ### hyp_schwa
            if i < len(charphon)-1:
                if phon in vowel_phons and next_phon in ["l", "m", "n"] and char not in ["e", "eh", "ee", "i", "ie"]:
                    for syl in self.syllables_target:
                        if syl.isinSyllable(chars_end_index) and syl.isinSyllable(next_chars_start_index): #applies within a syllable (e.g. Seiel, tuen)
                            candidate = list(self.target)
                            candidate[chars_end_index] += "e"
                            candidate = "".join(candidate)
                            e = PossibleError(category = "hyp_schwa", level="SL", morphconst= "na",  phon_ok = "coll", range=[chars_end_index, next_chars_start_index], candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1]+"e", id = err_id)
                            
                            possibleErrors.append(e)
                            err_id += 1
               
        
            
            ### sepH
            if char == "h" and phon == "<eps>":
                candidate = list(self.target)
                del candidate[chars_end_index]
                candidate = "".join(candidate)
                e = PossibleError(category = "sepH", level="SL", morphconst= "redun",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char.replace("h", "" ), id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
            
            ### hyp_sepH
            if i < len(charphon)-1:
                if char[-1] in vowels and next_char[0] in vowels:
                    for syl in self.syllables_target:
                        if syl.isinSyllable(chars_end_index) and not syl.isinSyllable(next_chars_start_index): #applies if <h> appears between two vowels at a syllable boundary
                            candidate = list(self.target)
                            candidate[chars_end_index] += "h"
                            candidate = "".join(candidate)
                            e = PossibleError(category = "hyp_sepH", level="SL", morphconst= "na",  phon_ok = "true", range=[chars_end_index, next_chars_start_index], candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1]+"h", id = err_id)
                            
                            possibleErrors.append(e)
                            err_id += 1
                            
            ### Cdouble
            
            #doubled consonants at morpheme boundaries occur as two phonemes in the BAS output, e.g. ? 'a n. n e:. m @ n, so each <n> is aligned with a separate [n] and they are not in one alignment unit
            #however, they have to be considered because there are some mistakes, e.g. <Dussel> for which BAS returns d 'U s. s @ l
            double_phon = False
            if i < len(charphon)-1 and char == next_char and char in consonants: double_phon = True
            
            if re.search(r"([bcdfgjklmnpqrstvwxz])\1", char) or char =="ck" or double_phon:
                
                if double_phon:
                    e = PossibleError(level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, next_chars_end_index], substituted_range=i, substituted = char, id = err_id)
                
                else:
                    e = PossibleError(level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, chars_end_index], substituted_range=i, substituted = char[1], id = err_id)
                
                candidate = list(self.target)
                del candidate[chars_start_index:chars_end_index]
                e.candidate = "".join(candidate)
                        
                morpheme_boundary = False
                
                for mor in self.morphemes_target:
                    
                    if not double_phon and (mor.isinMorpheme(chars_start_index) and not mor.isinMorpheme(chars_end_index)):  #not over morpheme boundaries (should already be captured by correct BAS output (2 phonemes)                       
                        morpheme_boundary = True
                        
                    elif double_phon and (mor.isinMorpheme(chars_start_index) and not mor.isinMorpheme(next_chars_end_index)): 
                        morpheme_boundary = True
                
                if re.match(r"h(a|ä)tte(st|t|n)?$", self.target): #<hatte> etc. are an exception, here BAS puts a morpheme boundary between the two <t>
                    morpheme_boundary = False
                
                #if morpheme_boundary == True:
                #    e.level = "MO"
                #    e.category = "ins_morphboundary"
                #    e.morphconst = "neces"
                #    possibleErrors.append(e)
                #    err_id += 1
                
                if morpheme_boundary == False:
                    
                    if not double_phon:
                        
                        derivfinal = False
                        
                        ### Cdouble_decofin
                        for mo in range(len(self.morphemes_target)-1):
                            if self.morphemes_target[mo].ismorend(chars_end_index) and self.morphemes_target[mo+1].type != "INFL":
                                e.category = "Cdouble_decofin"
                                possibleErrors.append(e)
                                err_id += 1
                                derivfinal = True
                        
                        
                        ### Cdouble_beforeC
                        if i < len(charphon)-1 and next_char[0] not in vowels and derivfinal == False:
                            e.category = "Cdouble_beforeC"
                            
                            possibleErrors.append(e)
                            err_id += 1
                            
                            if char == "ss":
                                candidate = list(self.target)
                                candidate[chars_start_index:chars_end_index+1] = "ß"
                                e1 = copy.deepcopy(e)
                                e1.candidate = "".join(candidate)
                                e1.substituted = "ß"
                                e1.morphconst = "neces"
                                e1.id = err_id
                                
                                possibleErrors.append(e1)
                                err_id += 1
                            
                        ### Cdouble_final
                        elif i == len(charphon)-1 and derivfinal == False:
                            
                            e.category = "Cdouble_final"
                            if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible): e.morphconst = "neces"
                            else: e.morphconst = "na"
    
                            
                            possibleErrors.append(e)
                            err_id += 1
                            
                            if char == "ss":
                                candidate = list(self.target)
                                candidate[chars_start_index:chars_end_index+1] = "ß"
                                e1 = copy.deepcopy(e)
                                e1.candidate = "".join(candidate)
                                e1.substituted = "ß"
                                e1.morphconst = "na"
                                e1.id = err_id
                                
                                possibleErrors.append(e1)
                                err_id += 1
                            
          
                        ### Cdouble_interV
                        elif i < len(charphon)-1 and derivfinal==False:
                            if next_char[0] in vowels:
                                e.category = "Cdouble_interV"
                                e.morphconst = "redun"
                                e.phon_ok = "false"
                            
                                # if there is only "pseudo schärfungsschreibung" = stress is on the second syllable, phon_ok is true  <'alle> vs.  <al'lein> and  <vie'lleicht>
                                for j in range(len(self.syllables_target)):
                                    syl = self.syllables_target[j]
                                    if j < len(self.syllables_target)-1: next_syl = self.syllables_target[j+1]
                                    
                                    if syl.isinSyllable(chars_start_index) and not syl.isinSyllable(chars_end_index) and next_syl.type == "stress": 
                                        e.phon_ok = "true"
                                        if char == "ss": e.phon_ok = "false"
                            
                                possibleErrors.append(e)
                                err_id += 1
                            
                                
                                if char == "ss":
                                    e.phon_ok = "false" # single intervocalic s is always voiced and thus always phon_ok false
                                    candidate = list(self.target)
                                    candidate[chars_start_index:chars_end_index+1] = "ß"
                                    e1 = copy.deepcopy(e)
                                    e1.candidate = "".join(candidate)
                                    e1.substituted = "ß"
                                    e1.morphconst = "redun"
                                    e1.id = err_id
                                   
                                    possibleErrors.append(e1)
                                    err_id += 1
                            
                    else: #if double_phon
                        if i > 0  and prev_char in vowels: #is usually always the case that prev_char in vowels but see exception target "Gggeist":
                        
                            if i < len(charphon)-2:
                                if charphon[i+2][0][0] in vowels:
                                
                                    e.category = "Cdouble_interV"
                                    e.morphconst = "redun"
                                    e.phon_ok = "false"
                                
                                    # if there is only "pseudo schärfungsschreibung" = stress is on the second syllable, phon_ok is true  <'alle> vs.  <al'lein> and  <vie'lleicht>
                                    for j in range(len(self.syllables_target)):
                                        syl = self.syllables_target[j]
                                        if j < len(self.syllables_target)-1: next_syl = self.syllables_target[j+1]
                                        
                                        if syl.isinSyllable(chars_start_index) and not syl.isinSyllable(char_indices[i+2][0]) and next_syl.type == "stress": 
                                            e.phon_ok = "true"
                                            if char == "ss": e.phon_ok = "false"
                                
                                    possibleErrors.append(e)
                                    err_id += 1
                                    
                                else:
                                    e.category = "Cdouble_beforeC"
                                    possibleErrors.append(e)
                                    err_id += 1 
                                
                            else:
                                e.category = "Cdouble_final"
                                if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible): e.morphconst = "neces"
                                else: e.morphconst = "na"
                                possibleErrors.append(e)
                                err_id += 1
                                
                            
                                   
                                               
                                
                                   
                                
            # t+z at the moment not aligned as tz
            if i < len(charphon)-1 and char == "t" and next_char == "z" and phon =="t" and next_phon =="s": 
                
                if i < len(charphon)-2 and charphon[i+2][0][0] in vowels:
                    candidate = list(self.target)
                    del candidate[chars_start_index:chars_end_index+1]
                    candidate = "".join(candidate)
                    e = PossibleError(category ="Cdouble_interV", level="SL", morphconst="redun", phon_ok = "false",  range=[chars_start_index, next_chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                    possibleErrors.append(e)
                    err_id += 1
                    
                elif i == len(charphon)-2:
                    candidate = list(self.target)
                    del candidate[chars_start_index:chars_end_index+1]
                    candidate = "".join(candidate)
                    e = PossibleError(category ="Cdouble_final", level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, next_chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                    if self.morph_const(chars_start_index, next_chars_end_index, Token.not_inflectible): e.morphconst = "neces"
                    else: e.morphconst = "na"
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                
                elif i < len(charphon)-2 and charphon[i+2][0][0] not in vowels:
                    candidate = list(self.target)
                    del candidate[chars_start_index:chars_end_index+1]
                    candidate = "".join(candidate)
                    e = PossibleError(category ="Cdouble_beforeC", level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, next_chars_end_index], candidate = candidate, substituted_range=i, substituted = "", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                        
                        
            ### ovr_Cdouble_afterVlong / del_morphboundary
            
            this_is_coda = False 
            for syllable in self.syllables_target:
                    if  syllable.isinSyllable(chars_start_index) and syllable.isincoda(self.target, chars_index):
                        this_is_coda = True
            if i > 0:
                # "a" can be tense or lax; if the current character is a consonant preceded by "a" and this consonant is the syllable coda, its a closed syllable, hence "a" is lax
                if char in consonants and charphon[i-1][1] in ["aI", "OY", "aU", "i:", "e:", "E:", "a:", "o:", "u:", "y:", "2:", "i", "e", "o", "u", "y", "2"] or char in consonants and  charphon[i-1][1] == "a" and this_is_coda == False: 
                    
                    # at the beginning of a morpheme which is not INFL or end of a morpheme which is not followed bei INFL, if preceded or followed by a vowel, it is not a ovr_Cdouble error but del_morphboundary
                    boundaryerror = False
                    for m in range(len(self.morphemes_target)):
                        mor = self.morphemes_target[m]
                        if m > 0 and mor.isinMorpheme(chars_start_index) and mor.ismorstart(chars_start_index) and mor.type != "INFL": #e.g. *<dannach>
                            if  self.morphemes_target[m-1].string[-1] in vowels:
                                boundaryerror = True
                        if m < len(self.morphemes_target)-1 and mor.isinMorpheme(chars_end_index) and mor.ismorend(chars_end_index) and  self.morphemes_target[m+1].type != "INFL": #e.g. *<wegglachen>
                            if  self.morphemes_target[m+1].string[0] in vowels:
                                boundaryerror = True
                    
                    
                    if boundaryerror == True:
                        candidate = list(self.target)
                        candidate[chars_start_index:chars_end_index+1] = char+char
                        substituted = char+char
                        candidate = "".join(candidate)
                        e = PossibleError(category ="del_morphboundary", level="MO", morphconst="neces", phon_ok = "false",  range=[chars_end_index,next_chars_start_index], candidate = candidate, substituted_range=i, substituted = substituted, id = err_id)
                
                    
                    else:
                        candidate = list(self.target)
                        if char == "z":
                            candidate[chars_start_index:chars_end_index+1] = "tz"
                            substituted = "tz"
                        elif char == "k":
                            candidate[chars_start_index:chars_end_index+1] = "ck"
                            substituted = "ck"
                        elif char == "ß":
                            candidate[chars_start_index:chars_end_index+1] = "ss"
                            substituted = "ss"
                        else:
                            candidate[chars_start_index:chars_end_index+1] = char+char
                            substituted = char+char
                        candidate = "".join(candidate)
                        e = PossibleError(category ="ovr_Cdouble_afterVlong", level="SL", morphconst="na",  phon_ok = "false", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = substituted, id = err_id)
                    
                    #phon_ok is true if  syllable is stressed and previous syllable ends with "a" ("spazieren", "kaputt")
                    for syllable in self.syllables_target:
                        if syllable.isinSyllable(chars_start_index) and syllable.type == "stress" and  charphon[i-1][1] == "a":
                            e.phon_ok = "true"
                        
                        
                    possibleErrors.append(e)
                    err_id += 1                
                        
                    
                        
            ### hyp_Cdouble / ovr_Cdouble_afterC   / del_morphboundary
            consonant_char = False
            if char in consonants or char == "ng":
                consonant_char = True
                       
            if i == 0 and consonant_char or  i > 0 and consonant_char and  charphon[i-1][0][0]  in consonants or  i > 0 and consonant_char and  charphon[i-1][1]  in set(["I", "E", "O", "U", "Y", "9", "@"]) or   i > 0 and consonant_char and  charphon[i-1][1] == "a" and this_is_coda == True:       
                    
                # at the beginning of a morpheme which is not INFL or end of a morpheme which is not followed bei INFL, it is not a ovr_Cdouble error but del_morphboundary
                boundaryerror = False
                for m in range(len(self.morphemes_target)):
                    mor = self.morphemes_target[m]
                    if m > 0 and mor.isinMorpheme(chars_start_index) and mor.ismorstart(chars_start_index) and mor.type != "INFL": #e.g. *<dannach>
                        if  self.morphemes_target[m-1].string[-1] in vowels:
                            boundaryerror = True
                    if m < len(self.morphemes_target)-1 and mor.isinMorpheme(chars_end_index) and mor.ismorend(chars_end_index) and  self.morphemes_target[m+1].type != "INFL": #e.g. wegglachen
                        if  self.morphemes_target[m+1].string[0] in vowels:
                            boundaryerror = True
                
                
                if boundaryerror == True:
                    if char != "h": #excludes e.g. gehholt -> geholt (counts as ovr_Vlong_short)
                        candidate = list(self.target)
                        candidate[chars_start_index:chars_end_index+1] = char+char
                        substituted = char+char
                        candidate = "".join(candidate)
                        e = PossibleError(category ="del_morphboundary", level="MO", morphconst="neces", phon_ok = "false",  range=[chars_end_index,next_chars_start_index], candidate = candidate, substituted_range=i, substituted = substituted, id = err_id)

                 
                if boundaryerror == False:   
                    
                    e = PossibleError(category ="hyp_Cdouble", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate = list(self.target)
                    if char == "k":
                        candidate[chars_start_index:chars_end_index+1] = "ck"
                        e.substituted =  "ck"
                    elif char == "z":
                        candidate[chars_start_index:chars_end_index+1] = "tz"
                        e.substituted =  "tz"
                    elif char == "ng":
                        candidate[chars_start_index:chars_end_index+1] = "ngg"
                        e.substituted =  "ngg"
                        e.category = "ovr_Cdouble_afterC"
                    else:
                        candidate[chars_start_index:chars_end_index+1] = char+char
                        e.substituted =  char + char
                    candidate = "".join(candidate)
                    e.candidate = candidate
                    
                    if i == len(charphon)-1:
                        if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible): #at the end of the word as in "Bus" but not "ab"
                            e.morphconst = "hyp"
                        
                        # special suffixes that can have consonant doubling
                        else:
                            for mor in self.morphemes_target:
                                if mor.isinMorpheme(chars_end_index) :
                                    if mor.type == "SFX":
                                        if mor.string == "nis" or mor.string == "in":
                                            e.morphconst = "hyp"
                        
        
                    if i == 0 or charphon[i-1][0][0] in consonants:
                        e.category = "ovr_Cdouble_afterC"
                        e.morphconst = "na"
                    
                    possibleErrors.append(e)
                    err_id += 1
                
             
            ### hyp_Cdouble_form
            if char == "ck":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = char[1]+char[1]
                candidate = "".join(candidate)
                e = PossibleError(category ="hyp_Cdouble_form", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char[1]+char[1], id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
            elif i < len(charphon)-1 and char == "t" and next_char == "z" and phon =="t" and next_phon =="s":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "z"
                candidate = "".join(candidate)
                e = PossibleError(category ="hyp_Cdouble_form", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "z", id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
            
            elif char == "ss":
                candidate = list(self.target)
                candidate[chars_start_index:chars_end_index+1] = "ßß"
                candidate = "".join(candidate)
                e = PossibleError(category ="hyp_Cdouble_form", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ßß", id = err_id)
                possibleErrors.append(e)
                err_id += 1                   
                
            
            ### Vlong
            
                
                ###Vlong_single_h
            if re.search(r"[aeouäöü]h", char) and char != "ieh":
                candidate = list(self.target)
                del candidate[chars_end_index]
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_single_h", level="SL", morphconst="na", phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char.replace("h", "" ), id = err_id)
                
                #morphconst: if end of morpheme = inherited syllable-separating h and morphconst = neces; otherwise vowel-lengthening h and morphconst = na
                for morpheme in self.morphemes_target:
                    if morpheme.isinMorpheme(chars_start_index) and morpheme.ismorend(chars_end_index) and not morpheme.type in Token.not_inflectible: #not e.g. "oh"
                        e.morphconst="neces"

                
                possibleErrors.append(e)
                err_id += 1
                
                ###Vlong_double_h
                candidate = list(self.target)
                candidate[chars_end_index] = candidate[chars_start_index]
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_double_h", level="SL", morphconst="na", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char.replace("h", char[0]), id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
                
                ###Vlong_h_single                
            if (phon in [ "e:", "E:", "a:", "o:", "u:", "y:", "2:", "e", "o", "u", "y", "2"] and len(char) == 1 or phon == "a" and a_in_open_syllable == True) and not (i <len(charphon)-1 and next_char in vowels): #long or short tense vowel and not followed by another vowel (see Leha -> Lea which is hyp_sepH)
                candidate = list(self.target)
                candidate[chars_start_index] = candidate[chars_start_index]+"h"
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_h_single", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char+"h", id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
                
                ###Vlong_double_single
                candidate = list(self.target)
                candidate[chars_start_index] = candidate[chars_start_index]+candidate[chars_start_index]
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_double_single", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char+char, id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
            
                
                ###Vlong_single_double
            if char in ["aa", "ee", "oo"]:
                
                candidate = list(self.target)
                candidate[chars_end_index] = ""
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_single_double", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char[0], id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
                ###Vlong_h_double
                candidate = list(self.target)
                candidate[chars_end_index] = "h"
                candidate = "".join(candidate)
                e = PossibleError(category = "Vlong_h_double", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = char[0]+"h", id = err_id)
                
                possibleErrors.append(e)
                err_id += 1
                
                
                ##Vlong_otherI
            if phon == "i:" or phon == "I" or phon == "i":
                e = PossibleError(category = "Vlong_otherI", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                
                candidate = list(self.target)
                candidate[chars_start_index: chars_end_index+1] = "ii"
                candidate = "".join(candidate)
                e.candidate = candidate
                e.substituted = "ii"
                
                possibleErrors.append(e)
                err_id += 1
                
                e1 = copy.deepcopy(e)
                candidate = list(self.target)
                candidate[chars_start_index: chars_end_index+1] = "iei"
                candidate = "".join(candidate)
                e1.candidate = candidate
                e1.substituted = "iei"
                e1.id = err_id
                possibleErrors.append(e1)
                err_id += 1

                
                
                e2 = copy.deepcopy(e)
                candidate = list(self.target)
                candidate[chars_start_index: chars_end_index+1] = "iie"
                candidate = "".join(candidate)
                e2.candidate = candidate
                e2.substituted = "iie"
                e2.id = err_id
                possibleErrors.append(e2)
                err_id += 1

                
                ##Vlong_i_ie
                if char == "ie":
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "i"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_i_ie", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "i", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ih_ie
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ih"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ih_ie", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ih", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ieh_ie
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ieh"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ieh_ie", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ieh", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                    
                ##Vlong_ie_i
                if char == "i" and "i" in phon: #also "Zigarette": i is not long but tense and not lax
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ie"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ie_i", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ie", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ih_i
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ih"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ih_i", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ih", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ieh_i
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ieh"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ieh_i", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ieh", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_i_ih
                if char == "ih":
                    
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "i"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_i_ih", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "i", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                    
                ##Vlong_ieh_ih
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ieh"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ieh_ih", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ieh", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ie_ih
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ie"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ie_ih", level="SL", morphconst="na",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ie", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                    
                ##Vlong_i_ieh -------> Vieh has to be an exception for morphconst? but see genitive: Viehes
                if char == "ieh":
                    
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "i"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_i_ieh", level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "i", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
            
                ##Vlong_ie_ieh
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ie"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ie_ieh", level="SL", morphconst="neces",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ie", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
                ##Vlong_ih_ieh
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ih"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "Vlong_ih_ieh", level="SL", morphconst="neces", phon_ok = "true",  range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ih", id = err_id)
                    
                    possibleErrors.append(e)
                    err_id += 1
            
            ### ovr_Vlong_short
            if phon in ["I", "E", "O", "U", "Y", "9", "@"] and char != "<eps>" or phon == "a" and a_in_open_syllable == False:
                e = PossibleError(category ="ovr_Vlong_short", level="SL", morphconst="na",  phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                candidate = list(self.target)
                
                #vowel+h
                candidate[chars_start_index:chars_end_index+1] = char + "h"
                e.candidate = "".join(candidate)
                e.substituted = char+ "h"
                possibleErrors.append(e)
                err_id += 1 
                
                #doubled vowel
                if phon != "I":
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = char + char
                    e1 = copy.deepcopy(e)
                    e1.candidate = "".join(candidate)
                    e1.substituted = char+ char
                    e1.id = err_id
                    possibleErrors.append(e1)
                    err_id += 1 
                 
              
                
                if phon == "I":
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "ie"
                    e2 = copy.deepcopy(e)
                    e2.candidate = "".join(candidate)
                    e2.substituted =  "ie"
                    e2.id = err_id
                    possibleErrors.append(e2)
                    err_id += 1 
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "ieh"
                    e3 = copy.deepcopy(e)
                    e3.candidate = "".join(candidate)
                    e3.substituted =  "ieh"
                    e3.id = err_id
                    possibleErrors.append(e3)
                    err_id += 1 
            
        
            ### final_devoice
            fd_found = False
            #applies when a normally voiced consonant appears in the coda of a syllable
            voice_devoice = {"b":"p", "d": "t", "g":"k", "w":"f"}
            for syllable in self.syllables_target:
                if syllable.isincoda(self.target, chars_index):
                    if char in voice_devoice.keys()and phon == voice_devoice[char] or char == "g" and phon == "C": #g-spirantization also falls under final devoicing (see Eisenberg,2006, p.321)
                        candidate = list(self.target)
                        candidate[chars_start_index:chars_end_index+1] = voice_devoice[char]
                        candidate = "".join(candidate)
                        e = PossibleError(category= "final_devoice", level="MO",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = voice_devoice[char], id = err_id)
                        fd_found = True
                    
                    elif char == "s": #Cases like  "Haus, Kasten" which could be mistakenly written as "Hauß, Kaßten"
                        candidate = list(self.target)
                        candidate[chars_start_index:chars_end_index+1] = "ß"
                        candidate = "".join(candidate)
                        e = PossibleError(category= "final_devoice", level="MO",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "ß", id=err_id)
                        fd_found = True
                    #--> is now del_clust!
                    #elif char == "ng":
                    #    candidate = list(self.target)
                    #   candidate[chars_start_index:chars_end_index+1] = "nk"
                    #    candidate = "".join(candidate)
                    #    e = PossibleError(category= "final_devoice", level="MO",  phon_ok = "coll", range=chars_index, candidate = candidate, substituted_range=i, substituted = "nk", id=err_id)
                    
                        
                    if fd_found == True:
                        e.morphconst = "na" #morph. cons. does not help if the character does not appear at a morpheme boundary as in "Erbse" wich is pronounced al "Erpse"
                        for morpheme in self.morphemes_target:
                            if  morpheme.ismorend(chars_end_index): 
                                e.morphconst="neces"
                        if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False:
                            e.morphconst = "na"
                        if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
      
                        possibleErrors.append(e)
                        err_id += 1
            
            ###hyp_final_devoice         
            #applies when a voiceless consonant appears in the coda of a syllable
            hfd_found = False
            devoice_voice = {"p":"b", "t": "d", "k":"g", "f":"w"}
            for syllable in self.syllables_target:
                    if syllable.isincoda(self.target, chars_index):
                        if char[-1] in devoice_voice.keys():
                            candidate = list(self.target)
                            
                            if len(char) == 1:
                                candidate[chars_start_index:chars_end_index+1] = devoice_voice[char]
                            
                            elif len(char) == 2 and char[0] == char[1] or char == "ck": # captures double letters
                                candidate[chars_start_index:chars_end_index+1] = devoice_voice[char[-1]] + devoice_voice[char[-1]] #last character matches "k" in  "ck" too
                            
                            candidate = "".join(candidate)
                            e = PossibleError(category= "hyp_final_devoice", level="MO", phon_ok = "true",  range=chars_index, candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)
                            hfd_found = True
                        
                        elif char == "ß": #Cases like  "hies" for "hieß"
                            candidate = list(self.target)
                            candidate[chars_start_index:chars_end_index+1] = "s"
                            candidate = "".join(candidate)
                            e = PossibleError(category= "hyp_final_devoice", level="MO", range=chars_index,  phon_ok = "true", candidate = candidate, substituted_range=i, substituted = "s", id=err_id)
                            hfd_found = True
                            
                        if hfd_found == True:
                            e.morphconst = "na" #morph. cons. does not help if the character does not appear at a morpheme boundary as in "Erbse" wich is pronounced al "Erpse"
                            for morpheme in self.morphemes_target:
                                if  morpheme.ismorend(chars_end_index): 
                                    e.morphconst="neces"
                            if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False:
                                e.morphconst = "na"
                            if self.morph_const_bound_morphemes(chars_start_index, Token.bound_morphemes): e.morphconst = "neces"
                            
                            possibleErrors.append(e)
                            err_id += 1
                        
            
            ### final_ch_g
            for syllable in self.syllables_target:
                if  char == "g" and syllable.isincoda(self.target, chars_index):
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "ch"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "final_ch_g", level="MO", morphconst="na",  phon_ok = "coll", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "ch", id = err_id)
                    
                    if i > 0 and charphon[i-1][1] == "I": #only here standard pronunciation allows g-spirantization
                        e.phon_ok = "true"
                     
                    for morpheme in self.morphemes_target:   
                        if  morpheme.ismorend(chars_end_index): 
                            e.morphconst="neces"
                        if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False:
                            e.morphconst = "na"
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
            ### hyp_final_g_ch
                if  char == "ch" and syllable.isincoda(self.target, chars_index):
                    candidate = list(self.target)
                    candidate[chars_start_index: chars_end_index+1] = "g"
                    candidate = "".join(candidate)
                    e = PossibleError(category = "hyp_final_g_ch", level="MO", morphconst="neces",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "g", id = err_id)
                    
                    for morpheme in self.morphemes_target:
                        if  morpheme.ismorend(chars_end_index): 
                            e.morphconst="neces"
                        if self.morph_const(chars_start_index, chars_end_index, Token.not_inflectible) == False:
                            e.morphconst = "na"
                    
                    possibleErrors.append(e)
                    err_id += 1
                    
            #### ins_morphboundary
            if i < len(charphon)-1:
                
                phon_voice_devoice = {"b":"p", "d": "t", "g":"k", "v":"f", "z":"s"}
                # applies when one morpheme ends with the same phoneme or grapheme as the next one starts with 
                if phon == next_phon:
                    for morpheme in self.morphemes_target:
                        if morpheme.isinMorpheme(chars_start_index) and not morpheme.isinMorpheme(next_chars_end_index): # means not in same morpheme
                            candidate = list(self.target)
                            del candidate[chars_end_index]
                            candidate = "".join(candidate)
                            possibleErrors.append(PossibleError(category= "ins_morphboundary", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "", id = err_id))
                            err_id += 1
                
                  
                
                elif char == "er" and phon == "6" and next_char == "r": #r following a vocalized r as in "Überraschung"
                    for morpheme in self.morphemes_target:
                        if morpheme.isinMorpheme(chars_start_index) and not morpheme.isinMorpheme(next_chars_end_index): # means not in same morpheme
                            candidate = list(self.target)
                            candidate[chars_start_index:chars_end_index+1] = "e"
                            candidate = "".join(candidate)
                            possibleErrors.append(PossibleError(category= "ins_morphboundary", level="MO", morphconst="neces",  phon_ok = "true", range=[chars_start_index, chars_end_index], candidate = candidate, substituted_range=i, substituted = "e", id = err_id))
                            err_id += 1   
                
                # if morpheme ends with voiceless consonant and next morpheme starts with voiced consonant
                elif i > 0 and  phon  in phon_voice_devoice and  charphon[i-1][1] == phon_voice_devoice[phon]:#e.g. "aufwachen"
                    for morpheme in self.morphemes_target:
                        if morpheme.isinMorpheme(chars_start_index) and not morpheme.isinMorpheme(char_indices[i-1][1]): # means not in same morpheme
                            candidate = list(self.target)
                            del candidate[chars_end_index]
                            candidate = "".join(candidate)
                            possibleErrors.append(PossibleError(category= "ins_morphboundary", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "", id = err_id))
                            err_id += 1
             
                elif i > 0 and charphon[i-1][0].endswith("ch") and char == "h": #e.g. nachause -> nachhause
                    for morpheme in self.morphemes_target:
                        if morpheme.isinMorpheme(chars_start_index) and not morpheme.isinMorpheme(char_indices[i-1][1]): # means not in same morpheme
                            candidate = list(self.target)
                            del candidate[chars_end_index]
                            candidate = "".join(candidate)
                            possibleErrors.append(PossibleError(category= "ins_morphboundary", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "", id = err_id))
                            err_id += 1
                            
            #### ins_wordboundary
            
            # applies if one word ends with the same grapheme as the next one starts with
            if i == len(charphon)-1: #last if this is the last grapheme of the word (e.g. *"is traurig")
                if self.contextlist != None:
                    for c in range(len(self.contextlist)-1):
                        if self.contextlist[c][0] == self.id:
                            next_word_target = self.contextlist[c+1][2]  
                            if next_word_target != "":
                                next_word_start_grapheme = next_word_target[0].lower()
                                
                                if char == next_word_start_grapheme:
                                    candidate = list(self.target)
                                    del candidate[chars_end_index]
                                    candidate = "".join(candidate)
                                    possibleErrors.append(PossibleError(category= "ins_wordboundary", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "", id = err_id))
                                    err_id += 1
             
                
            elif i == 0: #last if this is the first grapheme of the word (e.g. *"ist raurig")
                if self.contextlist != None:
                    for c in range(1,len(self.contextlist)):
                        if self.contextlist[c][0] == self.id:
                            prev_word_target = self.contextlist[c-1][2]  
                            prev_word_end_grapheme = prev_word_target[-1].lower()
                        
                            if char == prev_word_end_grapheme:
                                candidate = list(self.target)
                                del candidate[chars_end_index]
                                candidate = "".join(candidate)
                                possibleErrors.append(PossibleError(category= "ins_wordboundary", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "", id = err_id))
                                err_id += 1
            
            """
            # del_wordboundary --> shifted to unsystematic errors like del_C because it is hard to restrict which letters can be affected, maybe shift it back here when we have seen more examples
            if i == len(charphon)-1: #last character: ind der
                if self.contextlist != None:
                    for c in range(len(self.contextlist)-1):
                        if self.contextlist[c][0] == self.id:
                            next_word_target = self.contextlist[c+1][2]  
                            
            
            elif i == 0: #first character: Hand dam
                if self.contextlist != None:
                    for c in range(1,len(self.contextlist)):
                        if self.contextlist[c][0] == self.id:
                            prev_word_target = self.contextlist[c-1][2]  
                            prev_word_end_grapheme = prev_word_target[-1].lower()
                            if self.orig[char_a[0][0]].lower() == prev_word_end_grapheme:  
                                e.category = "del_wordboundary"                     
                                boundaryerror = True
            """
            
            ### MO_other
            
            #lässst -> lässt
            if char == "ss" and i < len(charphon)-1:
                for mor in self.morphemes_target:
                    if mor.isinMorpheme(next_chars_start_index) and mor.type == "INFL" and next_char == "t":
                        candidate = list(candidate)
                        candidate[chars_start_index: chars_end_index+1] = "sss"
                        candidate = "".join(candidate)
                        possibleErrors.append(PossibleError(category= "MO_other", level="MO", morphconst="neces",  phon_ok = "true", range=chars_index, candidate = candidate, substituted_range=i, substituted = "sss", id = err_id))
                        err_id += 1
    
                
            ### form
            no_form_error = False
            for err in possibleErrors: #ignore, if there was already a possible de_foreign error (e.g. häppy for happy is not analyzed as form but only de_foreign)
                if err.category == "de_foreign" and err.substituted_range == i:
                    no_form_error = True
            
            if no_form_error == False:
                form_dict = {"b":"d", "p":"q", "ä":"a", "ö":"o", "ü":"u", "d":"b", "q":"p", "a":"ä", "o":"ö", "u":"ü"}
                if char[0] in form_dict and self.target_isupper == False: #word was not written in uppercase
                    if  i > 0 or self.target_istitle == False: # letter was not uppercase
                        e = PossibleError(category ="form", level="PGII", morphconst="na",  phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                        candidate = list(self.target)
                        
                        if len(char) == 1:
                            candidate[chars_start_index:chars_end_index+1] = form_dict[char]
                        else: # first part of a diphtong or doubled vowel
                            candidate[chars_start_index] = form_dict[char[0]] 
                            e.range = [chars_start_index, chars_start_index]
                        if len(char) == 2 and char[0] == char[1]: # captures double letters
                            candidate[chars_start_index:chars_end_index+1] = form_dict[char[0]] + form_dict[char[0]]
                        
            
                        e.candidate = "".join(candidate)
                        e.substituted = e.candidate[chars_start_index:chars_end_index+1]
                        possibleErrors.append(e)
                        err_id += 1 
                 
                    #second part of a diphtong or doubled vowel  --> note: cannot be combined  in this system as diphtongs use one slot    
                if len(char) == 2 and char[1] in form_dict and self.target_isupper == False: #word was not written in uppercase
                    e = PossibleError(category ="form", level="PGII", morphconst="na",  phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate = list(self.target)
                    candidate[chars_end_index] = form_dict[char[1]] 
                    e.candidate = "".join(candidate)
                    e.range = [chars_end_index, chars_end_index]
                    e.substituted = e.candidate[chars_start_index:chars_end_index+1]
                    possibleErrors.append(e)
                    err_id += 1 
             
            
            ### voice
            voice_dict = {"b":"p", "d": "t", "g":"k", "w":"f", "s":"ß", "p":"b", "t": "d", "k":"g", "f":"w", "ß":"s" }
            for syllable in self.syllables_target:
                if syllable.isinSyllable(chars_start_index):
                    if not syllable.isincoda(self.target, chars_index): # in coda it would be final_devoice
                        if char[0] in voice_dict.keys() and char != "sch" and char != "ss":
                            candidate = list(self.target)
                                                      
                            if len(char) == 1:
                                candidate[chars_start_index:chars_end_index+1] = voice_dict[char]
                                
                                candidate = "".join(candidate)
                                e = PossibleError(category= "voice", level="PGII",morphconst="na", phon_ok = "false",  range=chars_index, candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)
                                possibleErrors.append(e)
                                err_id += 1 
                                
                        if char[-1] in voice_dict.keys() and char != "ss":                    
                            if len(char) == 2 and char[0] == char[1] or char == "ck": # captures double letters
                                candidate = list(self.target)
                                candidate[chars_start_index:chars_end_index+1] = voice_dict[char[-1]] + voice_dict[char[-1]] #last character matches "k" in  "ck" too
                
                                candidate = "".join(candidate)
                                e = PossibleError(category= "voice", level="PGII",morphconst="na", phon_ok = "false",  range=chars_index, candidate = candidate, substituted_range=i, substituted = candidate[chars_start_index:chars_end_index+1], id = err_id)
                                possibleErrors.append(e)
                                err_id += 1 
                       
            ### multigraph
            if char in set(["ch", "sch", "qu", "ng"]):
                candidate = list(self.target)
                
                if char == "ch":
                    e = PossibleError(category ="multigraph", level="PGII", morphconst="na", phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate[chars_start_index:chars_end_index+1] = "c"
                    e.candidate = "".join(candidate)
                    e.substituted = "c"
                    possibleErrors.append(e)
                    err_id += 1 
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "h"
                    e1 = copy.deepcopy(e)
                    e1.candidate = "".join(candidate)
                    e1.substituted = "h"
                    e1.id = err_id
                    possibleErrors.append(e1)
                    err_id += 1 
                    
                if char == "sch":
                    e = PossibleError(category ="multigraph", level="PGII", morphconst="na", phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate[chars_start_index:chars_end_index+1] = "sc"
                    e.candidate = "".join(candidate)
                    e.substituted = "sc"
                    possibleErrors.append(e)
                    err_id += 1 
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "s"
                    e1 = copy.deepcopy(e)
                    e1.candidate = "".join(candidate)
                    e1.substituted = "s"
                    e1.id = err_id
                    possibleErrors.append(e1)
                    err_id += 1 
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "sh"
                    e2 = copy.deepcopy(e)
                    e2.candidate = "".join(candidate)
                    e2.substituted = "sh"
                    e2.id = err_id
                    possibleErrors.append(e2)
                    err_id += 1 
                    
                if char == "qu":
                    e = PossibleError(category ="multigraph", level="PGII", morphconst="na", phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate[chars_start_index:chars_end_index+1] = "q"
                    e.candidate = "".join(candidate)
                    e.substituted = "q"
                    possibleErrors.append(e)
                    err_id += 1 
                    
                
                if char == "ng":
                    e = PossibleError(category ="multigraph", level="PGII", morphconst="na", phon_ok = "false", range=[chars_start_index, chars_end_index], substituted_range=i, id = err_id)
                    candidate[chars_start_index:chars_end_index+1] = "n"
                    e.candidate = "".join(candidate)
                    e.substituted = "n"
                    possibleErrors.append(e)
                    err_id += 1 
                    
                    candidate = list(self.target)
                    candidate[chars_start_index:chars_end_index+1] = "g"
                    e1 = copy.deepcopy(e)
                    e1.candidate = "".join(candidate)
                    e1.substituted = "g"
                    e1.id = err_id
                    possibleErrors.append(e1)
                    err_id += 1         
                      
        
        # restore upper/lowercase in target and candidate words   
        if self.target_istitle == True:
            self.target = self.target.title()
            for error in possibleErrors:
                error.candidate = error.candidate.title()
                if error.substituted_range == 0:
                    error.substituted = error.substituted.title()
                    
        elif self.target_isupper == True:
            self.target = self.target.upper()
            for error in possibleErrors:
                error.candidate = error.candidate.upper()
                error.substituted = error.substituted.upper()
        
    
                        
        self.possErrors = possibleErrors    
        return possibleErrors
    
    
    def getPossibleErrorCombisSinglePCU(self, *listoferrors):
        
        charphon = self.getAlignedTargetPhonemes()
        
        allerrors = []
        for e in listoferrors:
            allerrors.extend(e)
        new_allerrors = []
        
        #compute error combinations of two errors within a single pcu
        for i in range(len(allerrors)):
            for j in range(i+1, len(allerrors)):
                if allerrors[i].substituted_range == allerrors[j].substituted_range: # two errors pertain to same PCU
                    if len(allerrors[i].substituted) != len(allerrors[j].substituted): 
                        if allerrors[i].substituted != "" and allerrors[j].substituted != "": #no deletion in one of the PCUs (otherwise "Pinsl" and "Pinsehl" combine to "Pinshl"
                            if allerrors[i].category != allerrors[j].category: # not same error category already
                                if not allerrors[i].category.startswith("Vlong_i") and not allerrors[j].category.startswith("Vlong_i"):
                                    if not allerrors[i].category == "Vlong_otherI" and not allerrors[j].category== "Vlong_otherI":
                                        error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                                        for k in range(len(charphon)): # PCUs of target word
                                            if k == allerrors[i].substituted_range:
                                                error.substituted =  my_python_levenshtein.combine_errors_single_pcu(charphon[k][0], allerrors[i].substituted, allerrors[j].substituted) # compute candidate PCU of combined errors
                                                if all((pe.substituted.lower()) != error.substituted.lower() for pe in allerrors if pe.substituted_range == error.substituted_range): #if there is not already a single error that produces this candidate pcu
                                                    if error.substituted.lower() != charphon[k][0].lower(): #if it is not just a matter of capitalization (Ck and Kk for K would combine to k)
                                                        if error.substituted.lower() not in ["dh"]: #some exceptions (dh = d + th)
                                                            new_allerrors.append(error)
                    
                    #hyp_schwa combines with another category (e.g. in <tuhen> with Vlong_h_single) in that an <e> is added at the end of the PCU                                
                    if allerrors[i].category == "hyp_schwa":
                        error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                        error.substituted = allerrors[j].substituted + "e"       
                        new_allerrors.append(error)                
                    elif allerrors[j].category == "hyp_schwa":
                        error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                        error.substituted = allerrors[i].substituted + "e"
                        new_allerrors.append(error)
                        
                    #del fus can combine with another category (e.g. Hemmpd)
                    if allerrors[i].category == "del_clust" and not allerrors[i].substituted == "nk":
                        error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                        error.substituted = allerrors[j].substituted + allerrors[i].substituted[-1]     #add p, t, k (e.g. Hemmpd) 
                        new_allerrors.append(error)                
                    elif allerrors[j].category == "del_clust" and not allerrors[j].substituted == "nk":
                        error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                        error.substituted = allerrors[i].substituted + allerrors[j].substituted[-1]      
                        new_allerrors.append(error)   
                                         
        return new_allerrors
    
    #compute combinations of errors (multiple lists of errors can be given); set new = True each time you use another list than before
    def getPossibleErrorCombinations(self, *listoferrors, new = False):
        
        if new == False:
            if self.possErrorCombinations != None:
                return self.possErrorCombinations
            
        charphon = self.getAlignedTargetPhonemes()         
            
            
        allerrors = []
        for e in listoferrors:
            allerrors.extend(e)
        
        #for e in self.getPossibleErrors():
        #    print(e.category)
        #print(len(self.getPossibleErrors()))
        
        
        #to reduce the number of computed error combinations: only take those error candidates into account for which the erroneous substring appears in the orig word
        #e.g. for *<eß>/<es> -> ignore errors with error candidates ("substituted") <eh>, <ee> etc.
        allerrors = [error for error in allerrors if error.substituted.lower() in self.orig.lower()]
        
        
        # obsolete! Errors are now reduced by checking whether the erroneous substring of the error candidate appears in the orig spelling at all!
        # ignore certain error types for error combinations if there are more than x single errors (complexity!!)    
        #if len(self.getPossibleErrors()) >= 20: 
        #    allerrors = [error for error in allerrors if error.category not in [ "ovr_Cdouble_afterC"]]  # don't ignore "ovr_Vlong_short" -> vermiest/vermisst
        #if len(self.getPossibleErrors()) >= 30:
        #    allerrors = [error for error in allerrors if error.category not in ["ovr_Cdouble_afterC", "ovr_Cdouble_afterVlong", "voice", "form", "repl_marked_unmarked", "ovr_Vlong_short"]]
        

        """
        # jetzt ausgelagert in eigene Funktion!
        
        new_allerrors = []
        
        #compute error combinations of two errors within a single pcu
        for i in range(len(allerrors)):
            for j in range(i+1, len(allerrors)):
                if allerrors[i].substituted_range == allerrors[j].substituted_range: # two errors pertain to same PCU
                    if len(allerrors[i].substituted) != len(allerrors[j].substituted): # candidate PCUs must have differing lengths
                        if allerrors[i].substituted != "" and allerrors[j].substituted != "": #no deletion in one of the PCUs (otherwise "Pinsl" and "Pinsehl" combine to "Pinshl"
                            if allerrors[i].category != allerrors[j].category: # not same error category already
                                if not allerrors[i].category.startswith("Vlong_i") and not allerrors[j].category.startswith("Vlong_i"):
                                    error = PossibleError(category = "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), id= "pe"+str(allerrors[i].id)+","+"pe"+str(allerrors[j].id), phon_ok = "true", substituted_range=allerrors[i].substituted_range)   
                                    for k in range(len(charphon)): # PCUs of target word
                                        if k == allerrors[i].substituted_range:
                                            error.substituted =  my_python_levenshtein.combine_errors_single_pcu(charphon[k][0], allerrors[i].substituted, allerrors[j].substituted) # compute candidate PCU of combined errors
                                            if all((pe.substituted.lower()) != error.substituted.lower() for pe in allerrors if pe.substituted_range == error.substituted_range): #if there is not already a single error that produces this candidate pcu
                                                if error.substituted.lower() != charphon[k][0].lower(): #if it is not just a matter of capitalization (Ck and Kk for K would combine to k)
                                                    new_allerrors.append(error)
                                               
        new_allerrors.extend(allerrors)
        allerrors = new_allerrors
        """
        
        #compute error combinations of two errors within a single pcu
        single_PCU_errorcombis = self.getPossibleErrorCombisSinglePCU(self.getPossibleErrors())
        allerrors.extend([error for error in single_PCU_errorcombis if error.substituted.lower() in self.orig.lower()])
        
        #for e  in allerrors:
        #    print(e.category, e.substituted, e.candidate)
        
        error_combinations = []
        
    
        max_index = len(charphon)
        a = []
        
        # each aligned character will be its own list in this list
        for i in range(max_index):
            a.append([])
        
        # for each aligned character append the target character    
        for i in range(len(charphon)):
            a[i].append((charphon[i][0], None))#, None))
            
        
        # for each error, append the error to the list at the aligned character where a substitution took place
        for error in allerrors:
            a[error.substituted_range].append((error.substituted, error.id))#, error.phon_ok))   
        
        #print(a)
        
        # calculate all combinations on all positions
        l = list(itertools.product(*a))
        
        #print(len(l))
        
        
        #discard error combinations where the number of errors (= tuple[1] != None) is more than half of the number of PCUs of the word
        if len(l) > 1000000:
            l = [candidate for candidate in l if sum(tuple[1] != None for tuple in candidate)/len(candidate) < 0.5]
        
        #print(len(l))
        
        for element in l:
            
            e = PossibleError()
            
            #phon_ok = True
            
            # join all positions to get the candidate word
            e.candidate = "".join(tuple[0] for tuple in element if tuple[0] != "<eps>")
                    
            # append the error ids of the single errors
            idlist_string = ",".join("pe"+str(tuple[1]).lstrip("pe") for tuple in element if tuple[1] != None) # error combis of a single PCU already start with pe
            if len(idlist_string.split(","))< 2: 
                continue # ignore single errors and the correct version
            e.category = idlist_string
                       
            error_combinations.append(e)
            
                     
        # delete "error combinations" which accidentally result in the target form (e.g. "stehen" could be Vlong_h_single + sepH or "Seen" could be Vlong_double_single + schwa)
        error_combinations = [error for error in error_combinations if error.candidate != self.target] 
        
        
        
        # limit number of combinations per word to 4
        #error_combinations = [error for error in error_combinations if len(error.category.split(",")) < 5]
        
        if self.target_istitle:
            for error in error_combinations:
                error.candidate = error.candidate.title()
        elif self.target_isupper:
            for error in error_combinations:
                error.candidate = error.candidate.upper()
        
        
        self.possErrorCombinations = error_combinations    
        return error_combinations


    # returns a list of errors which result in a candidate that is fully written with GPK rules (e.g. "Fahrrad" -> "Farat")
    # The parameter "type" can be set to "full" or "coll": "coll" also includes "schwa" as a category which results in a GPK-form                                                   
    def getGPKform(self, type="full"):  
        # error categories which indicate that the word cannot be written with GPK rules only  
        categories_against_gpk = ["literal", "repl_unmarked_marked", 
                                  "vocR", "sepH", 
                                  "Cdouble_interV", "Cdouble_beforeC", "Cdouble_final", 
                                  "Vlong_i_ie", "Vlong_i_ih", "Vlong_i_ieh", 
                                  "Vlong_single_double", "Vlong_single_h",
                                  "final_devoice", "final_ch_g", "ins_morphboundary", "ins_wordboundary"]
        if type == "coll":
            categories_against_gpk.append("schwa")
        
        # get possible errors for the word  
        errors = self.getPossibleErrors()
        error_combis = self.getPossibleErrorCombinations(errors)

        # will be filled with tuples of (candidate, number of relevant categories) 
        gpk_candidates = []
        
        # single errors
        for error in errors:
            if error.category in categories_against_gpk:
                if error.category == "repl_unmarked_marked" and error.substituted == "eu": continue # discard eu/äu as the GPK form of äu/eu is oj/oi
                else:  gpk_candidates.append((error, 1))

        # error_combinations
        for error in error_combis:
            possible_candidate = True
            
            # resolve ids of single errors
            ids = [int(id.lstrip("pe")) for id in error.category.split(",")]
            # can only be a candidate if all categories indicate violation agains GPK
            for id in ids: 
                for e in errors:
                    if e.id == id and e.category not in categories_against_gpk:
                        possible_candidate = False
                    elif e.id == id and e.category == "repl_unmarked_marked" and e.substituted == "eu": # discard eu/äu as the GPK form of äu/eu is oj/oi
                        possible_candidate = False
            if possible_candidate == True:
                gpk_candidates.append((error, len(ids)))
        
        # the error (combinations) which comprises most relevant errors will be the final candidate(s)
        if gpk_candidates != []:
            numbers_of_relev_errors = [number for (candidate, number) in gpk_candidates]
            max_num = max(numbers_of_relev_errors)                             
            gpk_candidates = [candidate for (candidate, number) in gpk_candidates if number == max_num]
             
        return gpk_candidates   
    
    
    
    def setpossibleerrorsfromxml(self, xmlfile):
        self.possErrors = []
        self.possErrorCombinations = []
        
        tree = ET.parse(xmlfile)
        root = tree.getroot()
        
        for token in root.findall("target_token"):
            if token.get("id") == self.id:

                for posserr in token.find("possible_errors"):
                    e = PossibleError()
                    e.id = posserr.get("id")
                    e.category = posserr.get("category")
                    e.level =  posserr.get("level")
                    e.morphconst = posserr.get("morph_const")
                    e.range = posserr.get("range")
                    e.candidate = posserr.get("candidate")
                    #e.substituted_range = substituted_range #index in phoneme-aligned-character list
                    #e.substituted = substituted # substituted character(s)  
                    e.phon_ok = posserr.get("phon_ok")
                    self.possErrors.append(e)


                for posserr in token.find("possible_error_combinations"):
                    e = PossibleError()
                    e.candidate = posserr.get("candidate")
                    e.category = posserr.get("errors")
                    e.phon_ok = posserr.get("phon_ok")
                    self.possErrorCombinations.append(e)
        
        
    
    def geterrors(self):
        
        #for error in self.getPossibleErrors():
        #    print(error.substituted_range, error.category, error.substituted)
        
        if self.orig == self.target: 
            return []
        
        if self.errors != None:
            return self.errors
        
        errors = []
        
        found = False
        systematic_errors = False   
        characters_aligned = None
        
        err_id = 1
        
        #diffuse
        plain_target = re.sub("_|\-|\|", "", self.target)
        if len(plain_target) > 4 and Levenshtein.distance(self.orig, plain_target)/len(plain_target) > 0.66:
            e = PossibleError(category="diffuse", level="PGII", morphconst="na", phon_ok = "false", range=[0,len(self.target)-1], id = err_id)
            errors.append(e)
            err_id += 1
            self.errors = errors
            systematic_errors = True
            
        
        #repl_das_dass
        if self.orig.lower() == "das" and self.target.lower() == "dass":
            e = PossibleError(category="repl_das_dass", level="SN", morphconst="na", phon_ok = "true", range=[0,len(self.target)-1], id = err_id)
            errors.append(e)
            err_id += 1
            self.errors = errors
            systematic_errors = True
            
        elif self.orig.lower() == "dass" and self.target.lower() == "das":
            e = PossibleError(category="repl_dass_das", level="SN", morphconst="na", phon_ok = "true", range=[0,len(self.target)-1], id = err_id)
            errors.append(e)
            err_id += 1
            self.errors = errors
            systematic_errors = True
        
        
          
     
        # check if only a single error applies
        if errors == []:
            for pe in self.getPossibleErrors():
                if pe.candidate.lower() == self.orig.lower():
                    errors.append(pe)
                    self.errors = errors
                    systematic_errors = True
                    #return errors

            
        # if not, check if an error combination applies
        if errors == []:
            for pe in self.getPossibleErrorCombinations(self.getPossibleErrors()):
                if found == True: 
                    self.errors = errors
                    break ## more than one error-combinations are possible though!! Was einfallen lassen...
                    #return errors
                if pe.candidate.lower() == self.orig.lower():

                    ids = [int(id.lstrip("pe")) for id in pe.category.split(",")]
                    for id in ids:
                        for e in self.getPossibleErrors():
                            if e.id == id:
                                errors.append(e)
                                found = True 
                                systematic_errors = True
                
        
        
        ###if mixture of unsystematic errors and systematic errors applies
        pseudo_target = None
        single_error = None
        error_combi = None
        
        
        real_target = self.target
        real_orig = self.orig
        if errors == []:
            characters_aligned = self.align_orig_target()
            
            #first assume that Levenshtein distance between orig and target is the smallest
            min_levenshtein = Levenshtein.distance(self.orig.lower(), self.target.lower())
            #replace_used = False
            
            found_alternative = False
            
            #check if an error candidate with a single error has a smaller Lev.
            for pe in self.getPossibleErrors():
                
                                
                invalid = False
                if Levenshtein.distance(pe.candidate.lower(), self.orig.lower()) < min_levenshtein:        
                             
                    
                    # no changes in positions allowed where there is already an error ("Wasserhanen -> not Wasserhann + e between n and n)    
                    target_ranges = []
                    target_counter = 0
                    
                    t = 0
                    while t < len(self.target):
                        found = False
                        
                        #collect indices of characters which are affected by a systematic error
                        if t in range(pe.range[0], pe.range[1]+1):
                                for p in range(len(pe.substituted)):
                                    target_ranges.append(target_counter)
                                    target_counter += 1
                                    t += pe.range[1] - pe.range[0] + 1
                                    found = True
                        
                        if found == False:
                            target_counter += 1
                            t += 1
                   
                    #check if an edit operation would affect characters where a systematic error is already (in this case the sys error is not valid)    
                    for op in my_python_levenshtein.get_operations(pe.candidate.lower(), self.orig.lower()):
                        if op != None and op[1] in target_ranges: 
                            if pe.category not in ["Cdouble_interV", "Cdouble_beforeC", "Cdouble_final", "vocR", "hyp_schwa", "del_clust"]: # does no harm but is necessary e.g. in "Nuner" for "Nummer" #früher: op[0] != "replace" aber das hat zu "macht" -> "makt" -> mart geführt  
                                invalid = True                                                              # list may have to be adapted! Also in for pe in self.getPossibleErrorCombinations(): ...
                                                                                                            # vocR: valor -> verlor (vocR error spans over e + r (2 PCUs)
                                                                                                            # Vlong_h_single + hyp_schwa: tuhen -> tun would be necessary but Vlong_h_single deleted because of sachgt -> sagt. need an alternative...
                    if invalid == True:
                        continue
                    
                    
                    pseudo_target = pe.candidate
                    single_error = pe
                    min_levenshtein = Levenshtein.distance(pe.candidate.lower(), self.orig.lower())
                
            
            
            #check if an error candidate with an error combination has a smaller Levenshtein distance.
            for pe in self.getPossibleErrorCombinations():
                
                
                invalid = False

                # just a test to see if it works better if insertions/deletions are better than replacements:
                #here_replace = False
                #for op in my_python_levenshtein.get_operations(pe.candidate, self.orig):
                #    if op[0] == "replace":
                #        here_replace = True
                       
                if Levenshtein.distance(pe.candidate.lower(), self.orig.lower()) < min_levenshtein: # or Levenshtein.distance(pe.candidate, self.orig) == min_levenshtein and replace_used == True and here_replace == False:
    
                    ids = [int(id.lstrip("pe")) for id in pe.category.split(",")]
                    
                
                    target_ranges = [] # stores indices of characters that are affected by a systematic error
                    target_counter = 0
                    targetrange_category_dict = {} # stores indices of characters that are affected by a systematic error and the corresponding error category
                    
                    t = 0 #iterates through the target word
                    while t < len(self.target):
                        found = False
                        #get the single errors that consitute the error combination
                        for id in ids:
                            for e in self.getPossibleErrors():
                                if e.id == id:
                                    #if the current target character was affected by an error
                                    if t in range(e.range[0], e.range[1]+1):
                                        
                                        for p in range(len(e.substituted)):
                                            target_ranges.append(target_counter)
                                            targetrange_category_dict[target_counter] = e.category
                                            target_counter += 1
                                            t += e.range[1] - e.range[0] + 1 # was tut dies? ggf. checken!
                                            found = True
                        
                        if found == False:
                            target_counter += 1
                            t += 1
                   
                    
                    for op in my_python_levenshtein.get_operations(pe.candidate.lower(), self.orig.lower()):
                            for r in target_ranges:
        
                                    if op != None and op[1] in target_ranges:
                                        if targetrange_category_dict[op[1]] not in ["Cdouble_interV", "Cdouble_beforeC", "Cdouble_final", "vocR", "hyp_schwa", "del_clust"]: #Vlong_h_single deleted because of sachgt -> sagt
                                            invalid = True
                                            break
                    
                    if  invalid == True:
                        continue
                        
                    single_error = False
                    error_combi = pe
                    pseudo_target = pe.candidate
                    min_levenshtein = Levenshtein.distance(pe.candidate.lower(), self.orig.lower())
                    
                    # just a test to see if it works better if insertions/deletions are better than replacements:
                    #for op in my_python_levenshtein.get_operations(pe.candidate, self.orig):
                    #    if op[0] == "replace":
                    #        replace_used = True
                    
            # add the single error or error combination with the smalles Lev.-dist. if it is smaller than the Lev.-dist. of orig and target
            if single_error != False and single_error != None: 
                errors.append(single_error)
                
            elif single_error == False:
                ids = [int(id.lstrip("pe")) for id in error_combi.category.split(",")]
                for id in ids:
                    for e in self.getPossibleErrors():
                        if e.id == id:
                            errors.append(e)  #add all errors of combination

            
            # set pseudo_target as target (we pretend that the error candidate was the target in order now to find the unsystematical errors)
            if pseudo_target != None:
                #print("pseudo", pseudo_target)
                self.target = pseudo_target
        
                
        if errors != []:
            self.errors = errors
        
        #### 
        if pseudo_target != None:
            # example: orig = raurich, target = traurig, pseudo_target = traurich (final_ch_g)
            #align pseudo_target and real target via systematical errors
            self.orig = pseudo_target
            self.target = real_target
            self.characters_aligned = copy.deepcopy(self.align_orig_target())
            #align orig and pseudo_target
            self.orig = real_orig
            self.target = pseudo_target
            characters_aligned = mylevenshtein.align_levenshtein(self.orig, self.target)
            new_aligned = []
            
                    
            #print("self.characters_aligned", self.characters_aligned)
            #print("characters_aligned", characters_aligned)
            
            #combine both alignments (orig_pseudo = [orig, pseudotarget], pseudo_real = [pseudotarget, real target]) --> transitive
            last_t = 0
            
            empties = 0 #added for födSche - Pfötchen, since [['<eps>', '<eps>'], [0, 0]] of self.characters_alidnged is otherwise not taken over in new_aligned

            while empties < len(self.characters_aligned):
                # Marie
                while (empties < len(self.characters_aligned)) and (self.characters_aligned[empties][0] == ['<eps>', '<eps>']):
                    new_aligned.append(self.characters_aligned[empties])
                    empties += 1
                break
            
            for j in range(len(characters_aligned)):
                orig_pseudo = characters_aligned[j]
                if orig_pseudo[1] == ['<eps>', '<eps>']: #deletion necessary
                    new_aligned.append([orig_pseudo[0],['<eps>', '<eps>']]) 
                elif orig_pseudo[0] == ['<eps>', '<eps>']: #insertion necessary
                    for k in range(len(self.characters_aligned)):
                        pseudo_real = self.characters_aligned[k]
                        if orig_pseudo[1][0] in pseudo_real[0]:
                            new_aligned.append([['<eps>', '<eps>'], pseudo_real[1]])
                            last_t = pseudo_real[1][1]
                else:
                                        
                    for k in range(len(self.characters_aligned)):
                        pseudo_real = self.characters_aligned[k]
    
                        
                        #if pseudo_real[1][0] != '<eps>' and  orig_pseudo[1][1] != '<eps>' and pseudo_real[1][0] - orig_pseudo[1][1] > 1 and pseudo_real[0] == ['<eps>', '<eps>']:
                        #    new_aligned.append([['<eps>', '<eps>'], pseudo_real[1]])
                            
                        
                        if orig_pseudo[1][0] in pseudo_real[0]:
                                                        
                            if k > 0 and pseudo_real[1][0] != '<eps>' and pseudo_real[1][0] - last_t == 2 and self.characters_aligned[k-1][0] == ['<eps>', '<eps>']:
                                new_aligned.append([['<eps>', '<eps>'], self.characters_aligned[k-1][1]])
                            
                            new_aligned.append([orig_pseudo[0], pseudo_real[1]])
                            
                            if pseudo_real[1][1] != '<eps>':
                                last_t = pseudo_real[1][1]
                        
                        
                        
            #print("new_aligned", new_aligned)
            
            # restore n:1 relations: [[4, 4], [6, 6]], [[5, 5], [6, 6]] to [[4, 5], [6, 6]]
            new_new_aligned = [] # orig -realtarget?
            multiple_indices = []
            a = 0
            while a < len(new_aligned)-1:
                
                while a < len(new_aligned)-1 and new_aligned[a][1] == new_aligned[a+1][1] and new_aligned[a][1] != ['<eps>', '<eps>']: # n orig, 1 target
                    multiple_indices.append(new_aligned[a][0][0])
                    multiple_indices.append(new_aligned[a+1][0][0])
                    a += 1
    
                if  multiple_indices != []:
                    new_new_aligned.append([[multiple_indices[0], multiple_indices[-1]], new_aligned[a][1]])
                    multiple_indices = []
                    a += 1
                    
                else:
                    new_new_aligned.append([new_aligned[a][0], new_aligned[a][1]])
                    a += 1
                   
            if a < len(new_aligned): new_new_aligned.append([new_aligned[a][0], new_aligned[a][1]])
            
            #print("new_new_aligned", new_new_aligned)
            new_new_aligned = self.fix_diphthong_alignment(new_new_aligned)

            
            lev_aligned = copy.deepcopy(characters_aligned)
            lev_aligned = self.fix_diphthong_alignment(lev_aligned)
            #print("lev_aligned", lev_aligned)
            
            real_target_pseudo_aligned = copy.deepcopy(self.characters_aligned)
            real_target_pseudo_aligned = self.fix_diphthong_alignment(real_target_pseudo_aligned)
            #print("real_target_pseudo_aligned", real_target_pseudo_aligned)
            
            self.characters_aligned = copy.deepcopy(new_new_aligned)
         
            characters_aligned = copy.deepcopy(new_new_aligned) 

                
            ###############
        
    
        # if no errors found so far or errors are not only systematic, appply insert/delete/replace/switch errors
        if errors == [] or systematic_errors == False:
            if pseudo_target == None:
                characters_aligned = self.align_orig_target()
                characters_aligned = self.fix_diphthong_alignment(characters_aligned)
                
               

            else:
                characters_aligned = lev_aligned
                characters_aligned = self.fix_diphthong_alignment(characters_aligned)
            
                
            #print(self.orig)
            #print(self.target)
            swap = (False, 0)
            inserted = [] #stores where a character was already inserted to get the right index (and not always the same, e.g. missing c and h in "Weinatsbaum")
            
            if self.errors != None:
                for er in self.errors:#ins_clust errors also count as inserted (--> sepH/schwa also??)
                    if er.category == "ins_clust":
                        inserted.append(er.range)
            
            
            for i in range(len(characters_aligned)):
                char_a = characters_aligned[i]
                next_char_a = None
                prev_char_a = None
                
                if i < len(characters_aligned)-1:
                    next_char_a = characters_aligned[i+1]
                if i > 0:
                    prev_char_a = characters_aligned[i-1]
                
                #ins
                if char_a[0] == ["<eps>","<eps>"]:#orig doesn't exist
                    
                    
                    #exception: <vasteckt> for <versteckt> : only a vocR error which spans over 2 PCUs and no extra ins_V error!
                    r_voc_exception = False
                    if self.errors != None:
                        for err in self.errors:
                            if err.category == "vocR" and any(r in range(char_a[1][0], char_a[1][1]+1) for r in range(err.range[0], err.range[1]+1)):
                                r_voc_exception = True
                                err.phon_ok = "coll"
                    
                    
                    if r_voc_exception == False:
                    
                        e = PossibleError(level="PGIII", morphconst="na", phon_ok = "false", range=char_a[1], substituted_range=i, id = err_id)
                        
                        
                        # to get the aligned character index, look  which aligned unit the superfluous character belongs to
                        if pseudo_target != None:
                            #print("hier characters_aligned = orig - pseudo:", characters_aligned)
                            #print("hier new_new_aligned = orig - target ", new_new_aligned)
                            #print("hier real_target_pseudo_aligned = target - pseudo", real_target_pseudo_aligned)
                            for k in range(len(real_target_pseudo_aligned)): 
                                if real_target_pseudo_aligned[k][0] == char_a[1]:
                                    for j in range(len(new_new_aligned)): 
                                        
                                        #e.g. in "Huge"/"Hunger" (Pseudotarget "Huger"), target unit of insertion can be e.g. [4,5] so it is checked if pseudotarget corresponds to either of the two numbers
                                        if real_target_pseudo_aligned[k][1][0] == new_new_aligned[j][1][0] and [new_new_aligned[j][1]] not in inserted :
                                            e.range = new_new_aligned[j][1]
                                            inserted.append(new_new_aligned[j][1])
                                            break #important!
                                    
                                        elif real_target_pseudo_aligned[k][1][1] == new_new_aligned[j][1][1] and [new_new_aligned[j][1]] not in inserted :
                                            e.range = new_new_aligned[j][1]
                                            inserted.append([new_new_aligned[j][1]])
                                            break #important!    
                                        
                                                       
                        
                        
                        # ins_C
                        if self.target[char_a[1][0]].lower() in consonants: #changed from self.target (which is pseudotarget) here
                            e.category = "ins_C"
                            
                        # ins_V
                        elif self.target[char_a[1][0]].lower() in vowels:
                            e.category = "ins_V"
                        
                        
                        # punct
                        else:
                            e.category = "punct"
                            e.level = "PC"
                            e.phon_ok = "true"
                           
                        errors.append(e)
                        err_id += 1
                    
                
                   
                            
                #ins in cases of Dipthongs etc. (e.g. "wel" for "weil")
                
                elif ['<eps>', '<eps>'] not in char_a and char_a[1][1] - char_a[1][0] > 0 and self.orig[char_a[0][0]] in self.target[char_a[1][0]:char_a[1][1]+1]:
                                                                
                    e = PossibleError(level="PGIII", morphconst="na", phon_ok = "false", range=char_a[1], substituted_range=i, id = err_id)
                    
                    # to get the aligned character index, look  which aligned unit the superfluous character belongs to
                    if pseudo_target != None:
                        for j in range(len(new_new_aligned)): 
                            if new_new_aligned[j][0] == char_a[0] and new_new_aligned[j][1] not in inserted :
                                e.range = new_new_aligned[j][1]
                                inserted.append(new_new_aligned[j][1])
                                break
                    
                    if self.orig[char_a[0][0]].lower() == self.target[char_a[1][0]].lower():
                        if self.target[char_a[1][0]].lower() in consonants: #changed from self.target (which is pseudotarget) here
                            e.category = "ins_C"
                        else:
                            e.category = "ins_V"
                            
                    elif self.orig[char_a[0][0]].lower() == self.target[char_a[1][1]].lower():
                        if self.target[char_a[1][1]].lower() in consonants: #changed from self.target (which is pseudotarget) here
                            e.category = "ins_C"
                        else:
                            e.category = "ins_V"
                    
                    errors.append(e)
                    err_id += 1
                    
                    
                
                #del / split / merge
                elif char_a[1] == ["<eps>","<eps>"]:#target doesn't exist
                    e = PossibleError(level="PGIII", morphconst="na", phon_ok = "false", range="a"+str(i+1), substituted_range=i, id = err_id)
                    
                    # to get the aligned character index, look  which aligned unit the superfluous character belongs to
                    if pseudo_target != None:
                        for j in range(len(new_new_aligned)): 
                            if new_new_aligned[j][0] == char_a[0]:
                                e.range = "a"+str(j+1)
                    
                    boundaryerror = False
                    
                    # del_wordboundary
                    if i == len(characters_aligned)-1: #last character: ind der
                        if self.contextlist != None:
                            for c in range(len(self.contextlist)-1):
                                if self.contextlist[c][0] == self.id:
                                    next_word_target = self.contextlist[c+1][2]  
                                    next_word_start_grapheme = next_word_target[0].lower()
                                    if self.orig[char_a[0][0]].lower() == next_word_start_grapheme:
                                        e.category = "del_wordboundary"
                                        boundaryerror = True
                                        
                    
                    elif i == 0: #first character: Hand dam
                        if self.contextlist != None:
                            for c in range(1,len(self.contextlist)):
                                if self.contextlist[c][0] == self.id:
                                    prev_word_target = self.contextlist[c-1][2]  
                                    prev_word_end_grapheme = prev_word_target[-1].lower()
                                    if self.orig[char_a[0][0]].lower() == prev_word_end_grapheme:  
                                        e.category = "del_wordboundary"                     
                                        boundaryerror = True
                    
                                        
                    if boundaryerror == False:
                    
                        # del_C
                        if self.orig[char_a[0][0]].lower() in consonants:
                            e.category = "del_C"
                        
                        # del_V    
                        elif self.orig[char_a[0][0]].lower() in vowels:   
                            e.category = "del_V"
                        
                        # merge / ins_hyphen_lb
                        elif self.orig[char_a[0][0]] == "_":
                            
                            if self.eol == char_a[0] :
                                e.category = "ins_hyphen_lb"
                                e.level = "PC"
                                e.phon_ok = "true"
                                
                                # if previous character was not at the syllable boundary it is move_hyphen_lb in addition
                                if prev_char_a != None:
                                
                                    for syl in self.syllables_target:
                                        if syl.isinSyllable(prev_char_a[1][1]):
                                            if syl.range[1] != prev_char_a[1][1]:
                                                e2 = copy.deepcopy(e)
                                                e2.category = "move_hyphen_lb" #dab^bei               
                                                errors.append(e2)
                                                err_id += 1
                                
                            
                            else:
                                e.category = "merge"
                                e.level = "SN"
                                e.phon_ok = "true"
                               
                            #print(e.substituted_range)
                        
                        # split
                        elif self.orig[char_a[0][0]] == "|":
                            e.category = "split"
                            e.level = "SN"
                            e.phon_ok = "true"
                            
                        # hyphens
                        elif self.orig[char_a[0][0]] in ["-", "="]:
                            
                            e.level = "PC"
                            e.phon_ok = "true"
                            
                            if self.eol == char_a[0] :
                               
                               
                                # check if previous character was at the syllable boundary
                                if prev_char_a != None:
                                   
                                    
                                    for syl in self.syllables_target:
                                        if syl.isinSyllable(prev_char_a[1][1]) and syl.range[1] == prev_char_a[1][1]:
                                            e.category = "keep_hyphen_lb" #da-^bei
                                                
                                        else:
                                            e.category = "move_hyphen_lb" #dab-^ei
                                        
                            else:
                                if self.eol == prev_char_a[0]:
                                    e.category = "move_hyphen_lb" #da^-bei
                                    
                                else:
                                    e.category = "del_hyphen" #da-bei
                            
                        
                        # punct
                        else:
                            e.category = "punct"
                            e.level = "PC"
                            e.phon_ok = "true"
                            
                    
                    errors.append(e)
                    err_id += 1
                    
                    
                    
                #swap ->  doesn't work with Marcel's alignment tool...
                #if pseudo_target != None: 
                #    characters_aligned = lev_aligned
                elif i < len(characters_aligned)-1 and next_char_a[0] != ["<eps>","<eps>"] and next_char_a[1] != ["<eps>","<eps>"]:
                    if self.orig[char_a[0][0]].lower() != self.target[char_a[1][0]].lower() and  self.orig[next_char_a[0][0]].lower() != self.target[next_char_a[1][0]].lower() and self.orig[char_a[0][0]].lower() == self.target[next_char_a[1][0]].lower() and self.orig[next_char_a[0][0]].lower() == self.target[char_a[1][0]].lower(): #changed from self.target which is pseudotarget here
                        e = PossibleError(level="PGIII", morphconst="na", phon_ok = "false", range=[char_a[1][0],next_char_a[1][0]], substituted_range=i, id = err_id)
                        
                        # to get the swapped character index of the real target, look which character in the real target corresponds to the replace character in the pseudo target 
                        if pseudo_target != None:
                            for j in range(len(real_target_pseudo_aligned)):
                                if real_target_pseudo_aligned[j][0] == char_a[1]:
                                    e.range = [real_target_pseudo_aligned[j][1][0],real_target_pseudo_aligned[j+1][1][0]]                        
                        
                        if self.target[char_a[1][0]].lower() in consonants and self.target[next_char_a[1][0]].lower() in consonants:
                            e.category = "swap_CC"
                        elif self.target[char_a[1][0]].lower() in consonants and self.target[next_char_a[1][0]].lower() not in consonants:
                            e.category = "swap_CV"
                        elif self.target[char_a[1][0]].lower() not in consonants and self.target[next_char_a[1][0]].lower() in consonants:
                            e.category = "swap_VC"
                        elif self.target[char_a[1][0]].lower() not in consonants and self.target[next_char_a[1][0]].lower() not in consonants:
                            e.category = "swap_VV"
                        errors.append(e)
                        err_id += 1
                        swap = (True, i)
    
            #repl
            #if pseudo_target != None: 
            #    characters_aligned = lev_aligned          
            
            for i in range(len(characters_aligned)):
                char_a = characters_aligned[i]
                if i < len(characters_aligned)-1:
                    next_char_a = characters_aligned[i+1]
                
                
                char_a = characters_aligned[i]
                if char_a[0] != ["<eps>","<eps>"] and char_a[1] != ["<eps>","<eps>"]:
                    if char_a[0][0] < len(self.orig) and char_a[1][0] < len(self.target):
                        if self.orig[char_a[0][0]].lower() != self.target[char_a[1][0]].lower() and swap != (True, i) and swap != (True, i-1): #if it was not a case of swap
                            e = PossibleError(level="PGIII", morphconst="na", phon_ok = "false", range=char_a[1], substituted_range=i, id = err_id)
                            # to get the replace character index of the real target, look which character in the real target corresponds to the replace character in the pseudo target 
                            if pseudo_target != None:
                                for j in range(len(real_target_pseudo_aligned)): #statt self.characters_aligned
                                    if real_target_pseudo_aligned[j][0] == char_a[1]:
                                        e.range = real_target_pseudo_aligned[j][1]
    
                            
                            ### SL: other
            
                            #varstecken, soger
                            sl_other = False
                            if i < len(characters_aligned)-1:
                                char_a_index = char_a[1][0]
                                # Marie: to prevent index out of bounds
                                if char_a_index < len(self.target) and char_a_index < len(self.orig) and ((self.target[char_a_index].lower() == "e" and self.orig[char_a_index].lower() == "a") or (self.target[char_a_index].lower() == "a" and self.orig[char_a_index].lower() == "e")):
                                    if self.target[next_char_a[1][0]].lower() == "r": 
                                        for syl in self.syllables_target:
                                            if syl.isinSyllable(next_char_a[1][0]) and not syl.isinonset(self.target, next_char_a[1]):
                                                e.category = "SL_other"
                                                e.range = [char_a[1][0],next_char_a[1][1]] 
                                                sl_other = True
                                                for morpheme in self.morphemes_target:
                                                    if  morpheme.ismorend(next_char_a[1][1]): 
                                                            e.morphconst="neces"       
                                                if self.morph_const(next_char_a[1][0], next_char_a[1][1], Token.not_inflectible) == False: e.morphconst = "na"
                                                #bound morpheme
                                                if self.morph_const_bound_morphemes(next_char_a[1][0], Token.bound_morphemes): e.morphconst = "neces"
                                                errors.append(e)
                                                err_id += 1
                            
                            if sl_other == True: continue
                                
                            # repl_CC
                            if self.target[char_a[1][0]].lower() in consonants and self.orig[char_a[0][0]].lower() in consonants:
                                e.category = "repl_CC"
                            
                            # repl_VC    
                            elif self.target[char_a[1][0]].lower() in consonants and self.orig[char_a[0][0]].lower() in vowels:
                                e.category = "repl_VC"
                            
                            # repl_VV
                            elif self.target[char_a[1][0]].lower() in vowels and self.orig[char_a[0][0]].lower() in vowels:
                                e.category = "repl_VV"
                                
                            # repl_CV
                            elif self.target[char_a[1][0]].lower() in vowels and self.orig[char_a[0][0]].lower() in consonants:
                                e.category = "repl_CV"
                            
                        
                            # ins_hyphen_word
                            elif self.target[char_a[1][0]] == "-" and self.orig[char_a[0][0]] == "_":
                                e.category = "ins_hyphen_word"
                                e.level = "PC"
                                e.phon_ok = "true"
                            
                            
                            # (asterisks)
                            elif self.orig[char_a[0][0]] == "*" :
                                if self.target[char_a[1][0]].lower() in vowels :
                                    e.category = "repl_VV"  
                                elif self.target[char_a[1][0]].lower() in consonants :
                                    e.category = "repl_CC"  
                            
                            # punct
                            else:
                                e.category = "punct"
                                e.level = "PC"
                                e.phon_ok = "true"
                                
                            
                            errors.append(e)
                            err_id += 1
                
            if pseudo_target != None: characters_aligned = new_new_aligned  
            #char_a = characters_aligned[i]    
                
                 
        self.errors = errors
                
        # check if capitalization goes wrong
        if pseudo_target == None: characters_aligned = self.align_orig_target()
        for i in range(len(characters_aligned)):
            o = characters_aligned[i][0][0]
            o1 = characters_aligned[i][0][1]
            t = characters_aligned[i][1][0]
            if o != "<eps>" and t != "<eps>":
                if o < len(self.orig) and t < len(self.target):
                    if self.orig[o].islower() and self.target[t].isupper():
                        e = PossibleError(category = "low_up", level="SN", morphconst="na", phon_ok = "true", range=characters_aligned[i][1], substituted_range=i, id = err_id)
                        errors.append(e)
                        err_id += 1
                    elif not self.target_isupper:
                        if self.orig[o].isupper() or self.orig[o1].isupper():
                            
                            e = PossibleError( level="SN", morphconst="na", phon_ok = "true", range=characters_aligned[i][1], substituted_range=i, id = err_id)
                            
                            if i == 0 and not self.target[t].isupper():
                                e = PossibleError( level="SN", morphconst="na", phon_ok = "true", range=characters_aligned[i][1], substituted_range=i, id = err_id)
                                e.category = "up_low"
                                errors.append(e)
                                err_id += 1
                               
                            elif i != 0:
                                if o == 0: continue # for cases like "Fote" for "Pfote" where the learner correctly capitalized the first letter
                                
                                if characters_aligned[i-1][0][0] != "<eps>" and  self.orig[characters_aligned[i-1][0][0]] == "_": #for cases like "Bei_Spiel" where in the orig it was the beginning of a word
                                    e = PossibleError( level="SN", morphconst="na", phon_ok = "true", range=characters_aligned[i][1], substituted_range=i, id = err_id)
                                    e.category = "up_low"
                                    errors.append(e)
                                    err_id += 1
  
                                else:
                                    e = PossibleError( level="SN", morphconst="na", phon_ok = "true", range=characters_aligned[i][1], substituted_range=i, id = err_id)
                                    e.category = "up_low_intern"
                                    errors.append(e)
                                    err_id += 1
                                    
                           
                            
        ### restore alignments that got lost
        #insertions (target there/orig not) that were not transferred (e.g. "Fahraks" -> "Fahrrad")
        if pseudo_target != None:
            #print(pseudo_target)
            #print(new_new_aligned)
            n = 0
            end = len(new_new_aligned)-1
            while n < end:
                if new_new_aligned[n][1][1] != "<eps>" and new_new_aligned[n+1][1][0] != "<eps>": 
                        while new_new_aligned[n][1][1] - new_new_aligned[n+1][1][0] < -1: # = 'gap' in aligned characters because an insertion was not inherited [[3, 3], [2, 3]], [[4, 4], [5, 5]] (from "Bletn" -> "Betten" via "Betn") or even 2 characters as in "gesen" for "gesehen" (therefore we need the 2nd while loop!)
                            new_new_aligned.insert(n+1, [["<eps>", "<eps>"], [new_new_aligned[n][1][1]+1, new_new_aligned[n][1][1]+1]] )
                            #shift following errors to the right 
                            
                            for error in errors:
                                if error.substituted_range > n:
                                    try:
                                        error.range += 1 #single integer (does this apply anywhere?)
                                    except:
                                        try:
                                            error.range = "a"+str((int(error.range.lstrip("a"))) + 1) #already aligned character
                                        except:
                                            pass
                                            #error.range =[error.range[0]+1, error.range[1]+1] #start and end index separate
                            n+=1
                        
                        n+=1
                        
                else:
                    n+=1
            
            #print(new_new_aligned)
            
            ##insertions at end --> see if necessary
            end = len(new_new_aligned)-1
            if new_new_aligned[end][1][1] != "<eps>":
                if new_new_aligned[end][1][1] < len(real_target)-1:
                    new_new_aligned.append([["<eps>", "<eps>"], [new_new_aligned[end][1][1]+1, new_new_aligned[end][1][1]+1]] )
            #
            
            self.characters_aligned = new_new_aligned  
    
            #print("am Ende: new_new_aligned", new_new_aligned)   
            ###
        
        
        
        self.characters_aligned = self.fix_diphthong_alignment(self.characters_aligned)
        
        ### change error ranges that were set to aligned characters (e.g. "a5")  for ease (e.g. with category del) to "regular" error range, e.g. [4,5]:
        for error in errors:
            if str(type(error.range)) == "<class 'str'>":
                a = int(error.range.lstrip("a"))-1
                #print(self.characters_aligned)
                if self.characters_aligned[a][1] != ['<eps>', '<eps>']: ### IMPORTANT: with category del, where no target character is there, the error range refers to the original token!
                    error.range = copy.deepcopy(self.characters_aligned[a][1])
                else:
                    error.range = copy.deepcopy(self.characters_aligned[a][0])
            
        
        ### merge adjacent "del"-errors if they refer to a single grapheme rather than separate letters (e.g. <ch> in <dreffchen> for <treffen>)
        to_delete = []
        for error1 in errors:
            for error2 in errors:
                if error1.category.startswith("del_") and error2.category.startswith("del_"):
                    if error2.range[0] == error1.range[1] +1:
                        if self.orig[error1.range[0]:error1.range[1]+1] + self.orig[error2.range[0]:error2.range[1]+1] in ["ie", "ch", "ei", "ai", "au", "eu"]:
                            to_delete.append(error2)
                            error1.range[1] += 1
              
        errors = [error for error in errors if error not in to_delete]

        
                
        self.target = real_target
        self.errors = errors
        
        return errors
        
        
    
        
    def align_orig_target(self):
                
        if self.new_aligned != None:
            return self.new_aligned
        
        # indices of orig characters (left) and target characters (right)
        # <Daphl> and <Tafel> result in [[[1, 1], [1, 1]], [[2, 2], [2, 2]], [[3, 4], [3, 3]], [['#', '#'], [4, 4]], [[5, 5], [5, 5]]]
        aligned = [[] for i in range(len(self.getAlignedTargetPhonemes())) if self.getAlignedTargetPhonemes()[i][0] != "<eps>"]
        exception_categories = set(["SL_other", "diffuse", "ins_C", "ins_V", "del_C", "del_V", "repl_CV", "repl_VC", "repl_VV", "repl_CC", "swap_CC", "swap_CV", "swap_VC", "swap_VV",
                                    "ins_hyphen_lb", "ins_hyphen_word", "del_hyphen", "move_hyphen_lb", "keep_hyphen_lb", "punct", "del_wordboundary",
                                     "merge", "split", "up_low", "up_low_intern","low_up", "repl_das_dass", "repl_dass_das"])
        #schwa has to be included, otherwise problems with alignments e.g. for "komm" -> "kommen" if next letter is missing too
        eps = "<eps>"
        o = 0
        t = 0
        index = 0
        subst_range_handled = []
        
                
        if self.errors == None: 
            return self.characters_aligned
        
        elif all(error.category in exception_categories for error in self.errors):
            return self.characters_aligned
        
        
        # re-align words by systematical errors
        for i in range(len(self.getAlignedTargetPhonemes())):
            atp = self.getAlignedTargetPhonemes()[i][0] # = target characters
            if atp == "<eps>": continue
            
            err = False
            second_error = False
            #for error in self.errors:
                
            for j in range(len(self.errors)):
                error = self.errors[j]  
                
                #exception: up to now <tuen> for <tun> is aligned <ue>/<u> : the <e> is put separately here <u>/<u>, <e>/<eps> (same for e.g. Hempd/Hemd, del_clust and dannach/danach del_morphboundary)
                if error.substituted_range == i and error.category in ["hyp_schwa", "del_clust", "del_morphboundary"]: #e.g. <tuen> -> would also apply to hyp_sepH but these are always 3:2 mappings (schreihen/ eih -> ei, which are split further down in the code)
                    if not (error.category == "del_clust" and error.substituted == "nk"):
                        #if there is another error in the same PCU as hyp_schwa (e.g. <tuhen>)
                        for error2 in self.errors:
                            if error2.substituted_range == i and error2.category != error.category:
                                second_error = True
                                #first e.g.the vowel with its error (e.g. <uh> in <tuhen>) gets its own cell
                                aligned[index].append([o, o+len(error2.substituted)-1]) 
                                o += len(error2.substituted)
                                aligned[index].append([t, t+len(atp)-1])
                                t += len(atp)
                                    
                        if second_error == False:            
                            #first the vowel (e.g. <u> in <tun>) gets its own cell
                            aligned[index].append([o, o+len(atp)-1]) 
                            o += len(atp)
                            aligned[index].append([t, t+len(atp)-1])
                            t += len(atp)
                        
                        #an extra cell is added because the <e> is now separate
                        aligned.append([])
                        index += 1
                        
                        #then the <e> is aligned with <eps>
                        aligned[index].append([o, o])
                        o += 1
                        aligned[index].append([eps,eps])
                        err = True
                        subst_range_handled.append(i)
                            
                        
                elif error.substituted_range == i and error.category not in exception_categories: # if there was an error at this position, take ranges from substitutions
                                       
                    if i in subst_range_handled: continue # for error combinations which pertain to the same subst_range (pcu) only one error is regarded
                    err = True
                    subst_range_handled.append(i)
                    
                    for k in range(j+1, len(self.errors)):
                        if self.errors[j].substituted_range == self.errors[k].substituted_range: # two errors pertain to same PCU
                            error.substituted =  my_python_levenshtein.combine_errors_single_pcu(atp, self.errors[j].substituted,self.errors[k].substituted) # "substituted" is then the combination of both
                    if len(error.substituted) == 0: 
                        aligned[index].append([eps,eps])
                    else:
                        
                        aligned[index].append([o, o+len(error.substituted)-1])#orig_range
                        o += len(error.substituted)
                
                    aligned[index].append([t, t+len(atp)-1]) #target_range
                    t += len(atp)
                   
            
            if err == False: # if there was no error, just take the length of the target characters
                
                
                             
                aligned[index].append([o, o+len(atp)-1])
                o += len(atp)
                
                aligned[index].append([t, t+len(atp)-1])
                t += len(atp)
                
            index += 1
        
        
        
        new_aligned = []
        
        # to avoid 2:2 mappings (or n:n), separate mappings like ch, ch to c,c and h,h ([[5, 6], [8, 9]] to [[5,5], [8,8]],[[6,6], [9,9]] )
        for alignment in aligned:
            if alignment[0] == ["<eps>","<eps>"] or alignment[1] == ["<eps>","<eps>"]:
                new_aligned.append(alignment)
            elif alignment[0][1] - alignment[0][0] == 1 and alignment[1][1] - alignment[1][0] == 1: # 2:2, eg. ch
                new_aligned.append([[alignment[0][0], alignment[0][0]] ,[alignment[1][0], alignment[1][0]]])
                new_aligned.append([[alignment[0][1], alignment[0][1]] ,[alignment[1][1], alignment[1][1]]])
                
            elif alignment[0][1] - alignment[0][0] == 2 and alignment[1][1] - alignment[1][0] == 2 : # 3:3, eg. sch
                new_aligned.append([[alignment[0][0], alignment[0][0]] ,[alignment[1][0], alignment[1][0]]])
                new_aligned.append([[alignment[0][0]+1, alignment[0][0]+1] ,[alignment[1][0]+1, alignment[1][0]+1]])
                new_aligned.append([[alignment[0][1], alignment[0][1]] ,[alignment[1][1], alignment[1][1]]])
                
            elif alignment[0][1] - alignment[0][0] == 2 and alignment[1][1] - alignment[1][0] == 1 : # 3:2 eg. ieh/ie
                new_aligned.append([[alignment[0][0], alignment[0][0]] ,[alignment[1][0], alignment[1][0]]])
                new_aligned.append([[alignment[0][0]+1, alignment[0][0]+1] ,[alignment[1][0]+1, alignment[1][0]+1]])
                new_aligned.append([[alignment[0][1], alignment[0][1]] ,["<eps>","<eps>"]])
                
            elif alignment[0][1] - alignment[0][0] == 1 and alignment[1][1] - alignment[1][0] == 2 : # 2:3 eg. ie/ieh
                new_aligned.append([[alignment[0][0], alignment[0][0]] ,[alignment[1][0], alignment[1][0]]])
                new_aligned.append([[alignment[0][0]+1, alignment[0][0]+1] ,[alignment[1][0]+1, alignment[1][0]+1]])
                new_aligned.append( [["<eps>","<eps>"], [alignment[1][1], alignment[1][1]]])
                                    
                                    
            else:
                new_aligned.append(alignment) 
         
        
        #exception: sch/ch is different from ieh/ie in that for sch the first letter correpsonds to eps
        exception_alignment = []
        err_start = 0 #alignment unit where the sch/ch error starts
        
        for e in self.geterrors():
            if e.category == "PG_other" and e.substituted == "sch":
                
                for counter in range(len(new_aligned)):
                    if new_aligned[counter][1][0] == e.range[0]:
                        err_start = counter
        
             
                        #reorder alignment units so that eps is aligned with s
                        f = 0
                        while f != err_start:
                            exception_alignment.append(new_aligned[f])
                            f += 1
                            
                        exception_alignment.append([new_aligned[f][0], new_aligned[f+2][1]])
                        exception_alignment.append([new_aligned[f+1][0], new_aligned[f][1]])
                        exception_alignment.append([new_aligned[f+2][0], new_aligned[f+1][1] ])   
                        f += 3
                        
                        while f < len(new_aligned):
                            exception_alignment.append(new_aligned[f])
                            f += 1
                            
                        #print(exception_alignment)
                        new_aligned = exception_alignment

                        break
                 
        self.characters_aligned = new_aligned
        self.new_aligned = new_aligned
        
        
        #print(new_aligned)
        return new_aligned

    
    #add errors to learnerxml for a single token (slower than if it is done with a list of tokens)    
    def addErrorstoLearnerxml(self, in_learnerXMLfile):
        if self.geterrors() == []: return
        tree = ET.parse(in_learnerXMLfile)
        root = tree.getroot() 
        out = open(in_learnerXMLfile, mode = "w", encoding="utf-8")
        
        for token in root.iter("token"):
            if token.get("id") == self.id:
                
                # re-align orig and target (but only if errors were found, otherwise it does not work)
                chars_aligned = token.find("characters_aligned")
                for char_a in chars_aligned.findall("char_a"):
                    chars_aligned.remove(char_a)               
             
                aligned_chars = self.characters_aligned
                num_aligned = 1
                for alignment in aligned_chars:
                    xml_a_char = ET.SubElement(chars_aligned, "char_a", {"id":"a"+str(num_aligned)})
                    if not alignment[0] == ["<eps>","<eps>"]: 
                        xml_a_char.set("o_range", writexmlrange("o", alignment[0]))
                    if not alignment[1] == ["<eps>","<eps>"]:
                        xml_a_char.set("t_range", writexmlrange("t", alignment[1]))   
                    num_aligned += 1    
                
                # add errors
                xml_errors = token.find("errors")
                for error in self.geterrors():
                    a = False
                    xml_error = ET.SubElement(xml_errors, "err")
                    xml_error.set("cat", error.level + ":" + error.category)
                    #if error.range is already an aligned character like "a5" (as set for PG:ins/del/swap/replh) take this
                    if str(type(error.range)) != "<class 'list'>":
                        xml_error.set("range", error.range)
                        a = True
                    if a == False:
                        for char_a in chars_aligned.findall("char_a"): # otherwise match range of error in target word to id of aligned characters if applicable (usual case)
                            if char_a.get("t_range") == writexmlrange("t", error.range):
                                xml_error.set("range", char_a.get("id"))
                                a = True
                    if a == False: 
                        xml_error.set("range",  writexmlrange("t", error.range))# if not applicable use target range as error range (for instance for ie:ih)
                    xml_error.set("phon_orig_ok", error.phon_ok)
                    xml_error.set("morph_const", error.morphconst)
                
                        
        
        def prettify(elem): 
            """Return a pretty-printed XML string for the Element. http://stackoverflow.com/questions/17402323/use-xml-etree-elementtree-to-write-out-nicely-formatted-xml-files
            """
            rough_string = ET.tostring(elem, 'utf-8')
            rough_string = re.sub("\n", "", rough_string.decode("utf-8"))
            rough_string = re.sub("\t", "", rough_string)
            reparsed = xml.dom.minidom.parseString(rough_string)
            return reparsed.toprettyxml(indent="\t")  
            
        #tree.write(out_learnerXMLfile, "utf-8")
        print(prettify(root), file=out)                
    
    
    #computes key orthographic features of a target word and returns a dictionary with the features and their frequency in that word
    #The token object gets a further attribute "kofs" which contains a list of tuples [(kof1, target_range), (kof2, target_range), ...]
    #if errors is set to "True" not the features of a target word are computed but misspellings of the key orthographic features
    #these are also given as a dictionary (with a further keys "hyp" for hypercorrections and "other" for errors which don't correspond to a KOF) and in addition, 
    #every "PossibleError" object gets a further attribute "kof_cat" which carries the name of a key orthographic feature that was misspelled or "other"
    def keyOrthographicFeatures(self, errors = False):
        if errors == False:
            if self.kof_dict != None:
                return self.kof_dict
        
        if errors == True:
            if self.kof_error_dict != None:
                return self.kof_error_dict
        
        self.kofs = []
        
        kofs = {"graph_comb" : 0,
                "graph_marked" : 0,
                "ie" : 0,
                "schwa_silent" : 0,
                "doubleC_syl" :  0, 
                "doubleC_other" : 0, 
                "doubleV" : 0,
                "h_length": 0,
                "h_sep" : 0,
                "r_voc" : 0,
                "devoice_final" : 0,
                "g_spirant" : 0,
                "morph_bound" : 0,
                }
        
        #contains tuples of categories and ranges so that categories with multiple error candidates are still only considered once [(cat, [range, range]), ...]
        ranges_considered = []
        
        
        #select to either analyze possible errors of the target word or actually committed errors
        to_analyze = []
        if errors == True: 
            to_analyze = self.geterrors()
            kofs["hyp"] = 0 #add "hyp" errors
            kofs["other"] = 0 #add "other" errors
        
        else: to_analyze = self.getPossibleErrors()
        
        
        
        for e in to_analyze:
            
            found = False
            
            #ignore multiple categories on the same category and range when looking at possible errors
            if errors == False:
                if (e.category, e.range) in ranges_considered:
                    continue
                elif e.category.startswith("Cdouble") and ("Cdouble", e.range) in ranges_considered:
                    continue
        
        ### Schärfung / Other doubled consonants           
            if e.category.startswith("Cdouble") and not e.category == "hyp_Cdouble_form": #because this is always together with another Cdouble cat but the ranges may differ (esp. tz)
                    
                #schaerfung (with attention to stress pattern)
                for s in range(len(self.syllables_target)):
                    
                    syll = self.syllables_target[s] 
                    next_syll = None
                    
                    if s < len(self.syllables_target) -1:
                        next_syll = self.syllables_target[s+1] 
                    
                    #find syllable in which the Cdouble error occurred: either whole error range is in syllable range (<kommt>) or in case of syllable joint, error begins where syllable ends (<kommen>)
                    if e.range[0] in range(syll.range[0], syll.range[1]+1) and e.range[1] in range(syll.range[0], syll.range[1]+1) or e.range[0] == syll.range[1]: 
                        #throw out words in which the next syllable is stressed (<allein>)
                        if next_syll == None or next_syll.type != "stress":
                            #if category is Cdouble_interV, it is alwayss doubleC_syl
                            if e.category == "Cdouble_interV":
                                kofs["doubleC_syl"] += 1
                                ranges_considered.append(("Cdouble", e.range))
                                found = True
                                e.kof_cat = "doubleC_syl"
                            #otherwise throw out words in which the doubling is in a non-inflecting morpheme
                            else:
                                for m in self.morphemes_target:
                                    if m.isinMorpheme(e.range[0]):
                                        if m.ismorend(e.range[1]) and m.type not in Token.not_inflectible:
                                            kofs["doubleC_syl"] += 1
                                            ranges_considered.append(("Cdouble", e.range))
                                            found = True
                                            e.kof_cat = "doubleC_syl"
                                        else: 
                                            kofs["doubleC_other"] += 1
                                            ranges_considered.append(("Cdouble", e.range))
                                            found = True
                                            e.kof_cat = "doubleC_other"
                        else:
                            kofs["doubleC_other"] += 1
                            ranges_considered.append(("Cdouble", e.range))
                            found = True
                            e.kof_cat = "doubleC_other"            
            
        
        ### Vowel-lengthening h
            if e.category in ["Vlong_i_ih", "Vlong_single_h"] and e.morphconst == "na":
                kofs["h_length"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "h_length"
        
        ### Syllable separating h
            if e.category == "sepH":
                kofs["h_sep"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "h_sep"
        
            elif e.category in ["Vlong_single_h", "Vlong_i_ieh"] and e.morphconst == "neces": #e.g.<sieht>, <geht> but not <Vieh>
                kofs["h_sep"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "h_sep"
        
        ### Doubled vowels
            if e.category == "Vlong_single_double":
                kofs["doubleV"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "doubleV"
        
        ### ie
            if e.category in ["Vlong_i_ie", "Vlong_i_ieh"]:
                kofs["ie"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "ie"
        
        ### final devoicing
            if e.category == "final_devoice" and not e.substituted == "nk":
                kofs["devoice_final"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "devoice_final"
        
        ### vocalic r
            if e.category == "vocR" and e.phon_ok=="true":
                kofs["r_voc"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "r_voc"
        
        ### marked graphemes
            if e.category == "repl_unmarked_marked":
                kofs["graph_marked"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "graph_marked"
        
        ### phoneme combinations
            if e.category == "literal":
                kofs["graph_comb"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "graph_comb"
                
        ### silent schwa
            if e.category == "schwa" and e.phon_ok == "true":
                kofs["schwa_silent"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "schwa_silent"
                
        ### g-spirantization
            if e.category == "final_ch_g" and e.phon_ok == "true": #captures only "-ig" cases
                kofs["g_spirant"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "g_spirant"
                
        ### Morpheme boundaries
            if e.category == "ins_morphboundary":
                kofs["morph_bound"] += 1
                ranges_considered.append((e.category, e.range))
                found = True
                e.kof_cat = "morph_bound"
                
        
        ### in case of errors: 
            if errors == True and found == False:
                
        ### hypercorrections
                if e.category in ["repl_marked_unmarked", "hyp_sepH", "hyp_schwa", "hyp_vocR", "hyp_Cdouble", 
                                  "Vlong_ih_i", "Vlong_ieh_i", "Vlong_ie_i", "Vlong_double_single", "Vlong_h_single",
                                  "hyp_final_devoice", "hyp_final_g_ch", "del_morphboundary" ] or e.category == "ovr_Cdouble_afterVlong" and e.phon_ok =="true":
                    kofs["hyp"] += 1
                    ranges_considered.append((e.category, e.range))
                    found = True
                    e.kof_cat = "hyp"
            
            ### other errors
                else:
                    kofs["other"] += 1
                    e.kof_cat = "other"
        
        # set list of KOFs for each token [(kof1, target_range), ...]            
        if errors == False:
            for e in self.getPossibleErrors():
                if e.kof_cat != None:
                        self.kofs.append((e.kof_cat, e.range))
                    

        if errors == False: self.kof_dict = kofs
        else: self.kof_error_dict = kofs
          
        return kofs
