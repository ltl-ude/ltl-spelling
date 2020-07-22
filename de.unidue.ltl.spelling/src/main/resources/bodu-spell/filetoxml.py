#coding = utf-8
'''
Created on 20.05.2015

@author: Ronja Laarmann-Quante

This script produces a LearnerXML file without spelling errors for either pairs of original and target tokens or target tokens only. (Useful if errors are supposed to 
be annotated manually)

Wichtig: Für das Taggen muss ggf. der Arbeitsspeicher erhöht werden, 
dazu unter Windows: python/site-packages/nltk/tag/stanford.py und unter Linux /usr/local/lib/python3.4/dist-packages/nltk/tag/stanford.py

in der Zeile def __init__(self, model_filename, path_to_jar=None, encoding='utf8',verbose=False, java_options='-mx1000m'):
unter java_options einen höheren Wert eintragen (z.B. 4g für 4 GB)
[unter Linux als sudo]

'''

import os
import xml.etree.ElementTree as ET
import re
import xml.dom.minidom
import BAS_requests
import g2p
import argparse
import my_python_levenshtein
import tagger
import tag_morphemes

from marcel_levenshtein.WeightedLevenshtein import LevenshteinWeights
from marcel_levenshtein.Marcel_Levenshtein import LevenshteinAligner

import datetime


consonants = ["b","c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "z"]
vowels = ["a", "e", "i", "o", "u", "ä", "ö", "ü", "y"] # "y" as in "Typ"


#takes a list of start and end indices of a range like [0,4] and returns the equivalent range as appears in xml like p1..p5
def writexmlrange(letter, index_list):
    if index_list[0] == index_list[1]:
        return letter+str(index_list[0]+1)
    else:
        return letter+str(index_list[0]+1)+".."+letter+str(index_list[1]+1)

"""
#preliminary alignment of original and target token which is needed if errors are tagged manually
#input: orig and target token
#output list of 2-tuples; each tuple element is a list with two elements which give the range of the aligned characters ("#" for no character)
#example:input: orig=faln, target=fallen; output: [([0, 0], [0, 0]), ([1, 1], [1, 1]), ([2, 2], [2, 3]), ('#', [4, 4]), ([3, 3], [5, 5])]
def align(orig, target, withnm= False):
    aligned = marcel_levenshtein.leven_align.MainApplication(orig, target, param="gewichtungen.xml").run()
    aligned_indices = []
    c_orig = 1 #start index
    c_target = 1 #start index
    eps = "<eps>"
    
    for i in range(len(aligned[0])): # first alignment is used
        aligned[0][i] = list(aligned[0][i])
        o = aligned[0][i][0]
        t = aligned[0][i][1]
        
        #refinement of alignments to n:m mappings (currently never used)
        if withnm == True:
            if i < len(aligned[0])-1:
                aligned[0][i+1] = list(aligned[0][i+1])
                next_o = aligned[0][i+1][0]
                next_t = aligned[0][i+1][1]
                
                if not aligned[0][i] == [eps, eps]:
                    ## Fehlende Konsonantenverdopplung (Orig: falen, Target: fallen)
                    if t == next_t and o == eps: # (#, p) (p,p) wird zu (#, #) (p,pp)
                        aligned[0][i+1][1] = t+next_t
                        aligned[0][i][1] = eps
        
                    if t == next_t and next_o == eps: # (p, p) (#,p) wird zu (p,pp)(#, #) 
                        aligned[0][i+1][1] = eps 
                        aligned[0][i][1] = t+next_t    
                    
                    ## Konsonantenverdopplung zu viel
                    if o == next_o and t == eps: # (p, #) (p,p) wird zu (#, #) (pp,p)
                        aligned[0][i+1][0] = o+next_o
                        aligned[0][i][0] = eps
        
                    if o == next_o and next_t == eps: # (p, p) (p, #) wird zu (pp,p)(#, #) 
                        aligned[0][i+1][0] = eps 
                        aligned[0][i][0] = o+next_o
                    
                ## <a> statt <er>   
                if o == "a" and next_o == eps and t == "e" and next_t == "r": # (a, e) (#, r) wird zu (a, er), (#,#)
                    aligned[0][i][1] = t+next_t
                    aligned[0][i+1][1] = eps
    
                ## <er> statt <a>   
                if t == "a" and next_t == eps and o == "e" and next_o == "r": # (e, a) (r, #)  wird zu (er, a), (#,#)
                    aligned[0][i][0] = o+next_o
                    aligned[0][i+1][0] = eps
                
                ## <ch> statt <g>   
                if o == "c" and next_o == "h" and t == "g" and next_t == eps: # (c, g) (h, #)  wird zu (ch, g), (#,#)
                    aligned[0][i][0] = o+next_o
                    aligned[0][i+1][0] = eps
                
                ## <g> statt <ch>   
                if o == "g" and next_o == eps and t == "c" and next_t == "h": # (g, c) (#, h) wird zu (g, ch), (#,#)
                    aligned[0][i][1] = t+next_t
                    aligned[0][i+1][1] = eps
                    
                ## fehlendes <h> nach Vokal (aber nicht zwischen Vokalen wie bei seen -> sehen)
                if i < len(aligned[0])-2:
                    after_next_t = aligned[0][i+2][1]
                    if re.match(r"[aeiouäöü]", o.lower()) and next_o == eps and re.match(r"[aeiouäöü]", t.lower()) and next_t == "h" and not re.match(r"[aeiouäöü]", after_next_t.lower()): # (a, a) (#, h) wird zu (a, ah) (#,#)               
                        aligned[0][i][1] = t+next_t
                        aligned[0][i+1][1] = eps
                else:
                    if re.match(r"[aeiouäöü]", o.lower()) and next_o == eps and re.match(r"[aeiouäöü]", t.lower()) and next_t == "h":            
                        aligned[0][i][1] = t+next_t
                        aligned[0][i+1][1] = eps
                        
                ## <h> nach Vokal zu viel  (Köhnig -> König) (aber nicht zwischen Vokalen wie bei schreihen -> schreien)
                if i < len(aligned[0])-2:
                    after_next_t = aligned[0][i+2][1]
                    if re.match(r"[aeiouäöü]", o.lower()) and next_o == "h" and re.match(r"[aeiouäöü]", t.lower()) and next_t == eps and not re.match(r"[aeiouäöü]", after_next_t.lower()): # (a, a) (h, #) wird zu (ah, a) (#,#)               
                        aligned[0][i][0] = o+next_o
                        aligned[0][i+1][0] = eps
                else:
                    if re.match(r"[aeiouäöü]", o.lower()) and next_o == "h" and re.match(r"[aeiouäöü]", t.lower()) and next_t == eps :              
                        aligned[0][i][0] = o+next_o
                        aligned[0][i+1][0] = eps
                    
                ## <i> statt <ie> (Bine -> Biene)
                if o == "i" and next_o == eps and t == "i" and next_t == "e": # (i, i) (#, e) wird zu (i, ie) (#,#)               
                    aligned[0][i][1] = t+next_t
                    aligned[0][i+1][1] = eps
                    
                ## <ie> statt <i> (Tiger -> Tieger)
                if o == "i" and next_o == "e" and t == "i" and next_t == eps: # (i, i) (e, #) wird zu (ie, i) (#,#)               
                    aligned[0][i][0] = o+next_o
                    aligned[0][i+1][0] = eps
            
                
        o = aligned[0][i][0]
        t = aligned[0][i][1]
        
                
        if o != eps:
            if t == eps:
                aligned_indices.append(([c_orig, c_orig + len(o)-1], "#"))
                c_orig += len(o)
            else:
                aligned_indices.append(([c_orig, c_orig + len(o)-1], [c_target, c_target + len(t)-1]))
                c_orig += len(o)
                c_target += len(t)   
        else:
            if t == eps:
                continue
            aligned_indices.append(("#", [c_target, c_target + len(t)-1]))
            c_target += len(t)
    
    
    #print(aligned)
    #print(aligned_indices)
    return aligned_indices
"""


###########################################################################################################################################
#Input: Either a csv file with orig (left) and target (right) separated by a semicolon
#       or two aligned files orig and target (currently not used!)
#       or just a file with target words
#       or a list of words either of format [[target1, target2, target3, ...] or  [[orig1, target1], [orig2, target2], ...]
#if stanford_tag = True, POS tags of BAS are replaced with POS tags of Stanford tagger
#Output: LearnerXMLfile
def plaintolearnerxml(csv_file=None,orig_file=None, target_file=None, word_list = None, xml_filename=None, bas_file = None, user_bas_file = None, input_type="list", with_n_m_alignment=False, meta_tokens = ["\h"], stanford_tag = True):
    
    metamarks = {} #for tilde and questionmark appearing in target
    
    if csv_file == None and orig_file == None and target_file == None and word_list == None:
        print("Error: No files were given.")
        return
    
    if csv_file == None and orig_file != None and target_file == None:
        print("Error: An orig but no target file was given.")
        return
    
    if word_list != None and (csv_file != None or orig_file != None or target_file != None):
        print("Error: Both a list and a file were given as input.")
        return
    
    #get 'clean' filename 
    if csv_file != None:
        filename = re.sub("(.+/)+", "",  csv_file) #delete path
    elif target_file != None:
        filename = re.sub("(.+/)+", "",  target_file) #delete path
    elif xml_filename != None:
        filename = re.sub("(.+/)+", "", xml_filename)
    else:
        print("You have to specify an xml_filename")
        return
    filename = re.sub("((_.+)+)?\.\w+", "",filename) #delete _anything_after_filename.txt
    
    
    #handle annotations ? and ~ in target (given as file)
    def clean_targetannotations(target_file):
        target=open(target_file, mode="r", encoding="utf-8").read().strip().splitlines()
        for i in range(len(target)):
            if target[i].startswith("?") and re.search("\w", target[i]):
                target[i] = target[i].lstrip("?")
                metamarks[i] = "?"
            
            if target[i].startswith("~"):
                target[i] = target[i].lstrip("~")
                metamarks[i] = "~"

        print("\n".join(target), file=open(target_file, mode="w", encoding="utf-8"))
        
         
        
    #if orig and target given in separate files
    if csv_file == None and orig_file != None and target_file != None:
        orig = open(orig_file, mode="r", encoding="utf-8").read().strip().splitlines()
        clean_targetannotations(target_file)
        bas_input = target_file

        
    #if orig and target given in a csv-file    
    elif csv_file != None:
        origIN = open(csv_file, mode="r", encoding="utf-8").read().strip().splitlines()
        try:
            orig = [line.split("\t")[0] for line in origIN]
            target = [line.split("\t")[1] for line in origIN]
        except: 
            try:
                orig = [line.split(";")[0] for line in origIN]
                target = [line.split(";")[1] for line in origIN]       
            except:
                print("ERROR: Input file is not tab- or semicolon- separated") 
                return
                    
        print("\n".join(target), file=open("temporary.txt", mode= "w", encoding="utf-8"))
        clean_targetannotations("temporary.txt")
        bas_input = "temporary.txt"    
        
      
    #if only target given:
    elif csv_file == None and orig_file == None and target_file != None:
        clean_targetannotations(target_file)
        bas_input = target_file
        orig = None

    
    #if input is given as list (word_list)
    elif word_list != None and csv_file == None and orig_file == None and target_file == None:
        if all(len(line) == 2 for line in word_list):
            orig = [line[0] for line in word_list]
            target = [line[1] for line in word_list]
        else:
            orig = None
            target = word_list
        print("\n".join(target), file=open("temporary.txt", mode= "w", encoding="utf-8"))
        clean_targetannotations("temporary.txt")
        bas_input = "temporary.txt"
        
       
        
        
    #if BAS file is not given take information from the internet
    if bas_file == None:
        target = BAS_requests.getBAS(bas_input, input_type).strip() #handle semicolons that appeared in the target file
        target = re.sub(";;;XY;;;XY", "<semicolon>;;XY;<semicolon>;XY", target)  
        bas_dict = None

    else:

        words = open(bas_input, mode="r", encoding="utf-8").read().splitlines()
        target = []
        bas = open(bas_file, mode="r", encoding ="utf-8").read().strip().splitlines()
        
        if user_bas_file != None:
            user_bas_file = open(user_bas_file, mode="r+", encoding="utf-8")
            user_bas = user_bas_file.read().strip().splitlines()
            bas = bas + user_bas
         
            
        # make dictionary out of BAS list to speed up lookup
        # format: {"Dodo" : "Dodo;d ' o: . d o;NN;dodo;NN" }, target words are the keys and the complete BAS output as string the value
        bas_dict = {re.sub(" ", "", line.split(";")[0]):line for line in bas}
        
        for word in words:
            
            if word == "": #not target, e.g. in case of linebreak markers ^ which are only in the original
                target.append(";;;;")
                continue
            
            if word == ";":
                word = "<semicolon>"
               
            if word in bas_dict:
                target.append(bas_dict[word])
                
            #edit 18.1.18: annulled, because morphemes get different analyses e.g. for <fahrt> and <Fahrt>
            #edit 25.5.17: ignore titlecase (usually sentence initial) when looking up word 
            #we assume that titlecase/lowercase variant have the same properties, except for <weg>/<Weg>, <sucht>/<Sucht>,  but they are both included in the file 
            #thanks to Katrin for this code snippet
            #elif word.istitle() and word.lower() in bas_dict:
            #    bas_result = bas_dict[word.lower()]
            #    bas_result = bas_result[0].title() + bas_result[1:]
            #    target.append(bas_result) 
            #    bas_dict[word] = bas_result
                
            
            else:
                print("unknown:", word)
                bas_result = BAS_requests.getSingleBAS(word).strip()
                if bas_result == ";;;XY;;;XY":
                    bas_result = "<semicolon>;;XY;<semicolon>;XY" 
                target.append(bas_result)
                bas_dict[word] = bas_result #for quicker access if word appears more than once in the same text
                if user_bas_file != None: print(bas_result, file= user_bas_file) 

        
        # alter Abrufweg mit Listen, wahrscheinlich löschbar:
        
        #for i in range(len(words)):#
        #    found = False#
        #    for line_string in bas:#ausrücken
        #    #target.append(line_string) ####
        #        if re.sub(" ", "", line_string.split(";")[0]) == words[i]:#
        #            target.append(line_string)#
        #            found = True#
        #            break#  
        #                
        #    if found == False:# 
        #        print("unknown:", words[i])#
        #        bas_result = BAS_requests.getSingleBAS(words[i]) #
        #        print(bas_result)
        #        target.append(bas_result)#
        #        bas.append(bas_result) #for quicker access if word appears more than once
        #        if user_bas_file != None: print(bas_result, file= user_bas_file) #
                
                
        target = "\n".join(target)

    try: os.remove("temporary.txt")
    except: pass


    # Organize BAS output in list
    table=[]
    
    
    
    
    # new: syllables are now in the main BAS file!! the syllable file has the form  "Dodo;d ' o: . d o" per line
    #if syllable_file != None:
    #    syllable_file = open(syllable_file, mode = "r", encoding = "utf-8").read().splitlines()    
        
    #    if user_syllable_file != None:
    #        user_syllable_file_handle = open(user_syllable_file, mode ="r+", encoding ="utf-8")
    #        user_syllable = user_syllable_file_handle.read().splitlines()
    #        syllable_file = syllable_file + user_syllable
    #        user_syllable_file_handle.close()
        
        # make dictionary out of syllable file of the form {Dodo:d'o:.do}
        #syllable_dictionary = {re.sub(" ", "", line.split(";")[0]):re.sub(" ", "", line.split(";")[1]) for line in syllable_file if len(line.split(";")) == 2}
    
         
    for i in range(len(target.split("\n"))):
        
        #print(target.split("\n"))
        
        line = target.split("\n")[i] 
        line = line.split(";")        
        
        
        if line[0] == "<semicolon>":
            line = [';', '','XY', ';', 'XY']
            table.append(line)
            continue
        
        if line == ['', '', '', '', '']: # if there is no target (e.g. in cases of linebreak markers ^ which are only in the original)
            table.append(line)
            continue
        
        if re.sub(" ", "", line[0]) in meta_tokens: # for us \h
            line = [re.sub(" ", "", line[0]), "", "", "", ""]
            table.append(line)
            continue
        
        #without syllable info, BAS output for <.> is [['.', '_', 'XY', '.', 'XY', '.']] but with syllable info  [['. _ _', "'< P >", 'XY', '.', 'XY', '.']] which makes problems
        if not any(c.isalpha() for c in line[0]):
            line[1] = ""

            
            
        #print(line)
           
        graphemes = re.sub(" ", "", line[0])
        phonemes = re.sub("\.", "", line[1]) #new: syllables and stress marks are included in the main BAS file: here they are removed to obtain phonemes only 
        phonemes = re.sub("'", "", phonemes)
        phonemes = re.sub(" ", "", phonemes)
        
        # align graphemes and phonemes
        g2p_aligned_tuples = g2p.g2p_align(graphemes, phonemes)
        #print(g2p_aligned_tuples)
        g2p_aligned_tuples = g2p.refine_g2p_align(g2p_aligned_tuples)
        #print(g2p_aligned_tuples)
        g2p_aligned_tuples_withsyllables = g2p.insert_syllables(g2p_aligned_tuples, bas_dictionary = bas_dict)#[0] # returns tuple ( (graphemes,phonemes), updated syllable dictionary)
        #print(g2p_aligned_tuples_withsyllables)
        
        graphemes = " ".join(g for (g,p) in g2p_aligned_tuples_withsyllables)
        phonemes_with_syllables = " ".join(p for (g,p) in g2p_aligned_tuples_withsyllables)
        
        graphemes = re.sub("<eps>", "_", graphemes)
        phonemes_with_syllables = re.sub("<eps>", "_", phonemes_with_syllables)
        
        
        
        
        
        table.append([graphemes, phonemes_with_syllables, line[2], line[3], line[4]])
        
        
    # Append original xmltoken to BAS table if original xmltoken were given
    if orig != None:    
        try:
            for i in range(len(orig)):
                table[i].append(orig[i])
        except:
            print("Error: Number of original tokens and number of target tokens is not the same!")
            
  
    if stanford_tag == True:
        #POS-Tags: substitute those of BAS with those of tagger
        targets = [re.sub("\s|_", "", elem[0]) for elem in table] #attention: ^ is not included in this list because target is empty ""
        
        # POS-Tagging with Stanford-Tagger (currently not working)
        # tagged_targets = tagger.tag_list(targets, tagger_type="kids", correct_format=True) #output: [('Das', 'PDS'), ('ist', 'VAFIN'), ('ein', 'ART'), ('einfacher', 'ADJA'), ('Test', 'NN'), ('zum', 'APPRART'), ('\\h', 'XY'), ('Ausprobieren', 'NN')]

        # Workaround: filling tagged_target list with dummy POS-Tags
        # output: [('Das', 'NA'), ('ist', 'NA')....
        tagged_targets = []

        for i, target in enumerate(targets):
            item = "('" + target + "', 'NA')"
            tagged_targets.append(item)


        pos_list = [elem[1] for elem in tagged_targets] #['PDS', 'VAFIN', 'ART', 'ADJA', 'NN', 'APPRART', 'XY', 'NN']
    
        table_counter = 0
        pos_counter = 0
        while table_counter < len(table):
            if table[table_counter][0] != "": # since ^ has no target, this is skipped
                table[table_counter][2] = pos_list[pos_counter]
                table_counter += 1
                pos_counter += 1
            else:
                table_counter += 1
        
           
    #table_backup = open("table_backup.txt", mode="w", encoding="utf-8")
    #print(table, file=table_backup)
    
    ################
    #table = ast.literal_eval(open("table_backup.txt", mode="r", encoding="utf-8").read())
    ################
    
    # Create BAS output file and print BAS output to separate file
    #if bas_file == None:
    #    bas_output = open(filename+"_bas.txt", mode="w", encoding="utf-8") 
    #    print(target, file = bas_output)
    
    
    
    print("Prepating output file", datetime.datetime.now())  
    
        
    # Create xml output file
    if xml_filename == None: created_xml = open(filename+".xml", mode="w")
    else: created_xml = open(xml_filename, mode="w")
    
        
    # word = table[i] --> each element represents information for one word
    # word[0] = target-orthography
    # word[1] = target-sampa (word[0] and word[1] are aligned; spaces mark boundaries of corresponding characters ['h a ll o', "h 'a. l o:"]
    # word[2] = target POS-tag
    # word[3] = target morphemes
    # word[4] = target morpheme POS-tags
    # (word[5] = original orthography if given)
    
    #Create xml root
    xml_tokens = ET.Element("tokens", {"id":filename})
    
    tok_counter = 1
    
    for i in range(0,len(table)):
        word = table[i]
        
             
        #Alert if there are more graphemes than phonemes or vice versa
        if len(word[0].split()) != len(word[1].split()) and word[0] not in metamarks and word[0] != "\\h" and word[0] !=";":
            print(word[0] + " and " + word[1] +" are not aligned correctly!")
        
        
        if orig != None:     
            orig_word = re.sub("\^", "", word[5])
        else: orig_word = None
        
        # remove blanks in the target graphemes (BAS splits all characters)    
        target_word = re.sub("\s", "", word[0])
        target_word = re.sub("_", "", target_word)
        
        #store where the next token is ^ (end of line) or \h (end of headline)
        eol = False
        eoh = False
        if i < len(table)-1:
            if orig != None:
                next_orig = table[i+1][5]
                if next_orig == "^":
                    eol = True
            next_target = table[i+1][0]
            if next_target =="\h":
                eoh = True
        
        ###### <xmltoken> 
        if target_word in meta_tokens or target_word == "": # \h, ^ are ignored
            continue
        xml_token = ET.SubElement(xml_tokens, "token", {"id":"tok"+str(tok_counter), "target": target_word})
        tok_counter += 1
        if orig_word != None:
            xml_token.set("orig", orig_word) #delete ^ within orig word
        if i in metamarks:
            if metamarks[i] == "~":
                xml_token.set("target_comments", "ungram")
            elif metamarks[i] == "?":
                xml_token.set("target_comments", "unclear/onom")
        
        #### POS
        xml_token.set("pos_stts", word[2])
        
        ##### realword
        if orig_word != None and orig_word != target_word:
            with open("childlex/childlex_0.17.01_litkey_types.txt", mode="r", encoding="utf-8") as childlex_lexicon:
                childlex_lexicon = set(childlex_lexicon.read().splitlines())
            childlex_lexicon_lower = set([c.lower() for c in childlex_lexicon])
            
            #remove linebreak marks and hyphens at linebreaks (these are ignored when looking up the word in childlex)
            orig_word_transformed = re.sub(r"-\^", "", orig_word)
            orig_word_transformed = re.sub(r"\^-", "", orig_word_transformed)
            orig_word_transformed = re.sub(r"\^", "", orig_word_transformed)
            
            #the first token with a split mark is concatenated with the subsequent token(s) and this concatenation is evaluated
            # e.g. "auf| jeden| fall: the first token is analyzed as "aufjedenfall" but the others are treated separately ("jeden" and "fall") 
            if "|" in orig_word_transformed and i < len(table) and orig != None:
                if not (i > 0 and "|" in table[i-1][5]): #if the preceeding word already had a split mark, no concatenation takes place but only this word is analyzed
                    n = 1
                    orig_word_transformed = orig_word_transformed + table[i+n][5]
                    while "|" in table[i+n][5] and i+n < len(table): #collect all parts of the word that were written together
                        orig_word_transformed = orig_word_transformed + table[i+n+1][5]
                        n += 1
                    
            orig_word_transformed = re.sub(r"\|", "", orig_word_transformed)
            #print(orig_word, orig_word_transformed)
            
            exist_orig = all(part in childlex_lexicon_lower for part in re.split(r"[_-]", orig_word_transformed.lower())) #split word at - or _ and see if all parts exist in childlex
            if exist_orig: xml_token.set("realword", str(exist_orig).lower()) #only include when "True"
            
        
        ###### <characters_orig> 
        if orig_word != None:
            xml_chars_orig = ET.SubElement(xml_token, "characters_orig")
            orig = word[5] #again including ^
            o_counter = 1
            
            for j in range(0, len(orig)):   
                
                if orig[j] == "^":
                    continue
                          
                xml_char_o = ET.SubElement(xml_chars_orig, "char_o", {"id":"o"+str(o_counter)})
                o_counter += 1
                xml_char_o.text = orig[j]
                
                if j < len(orig)-1 and orig[j+1] == "^":
                    xml_char_o.set("layout", "EOL")
                
                if j == len(orig)-1: #set end of line/headline at last character if applicable
                    if eol == True: 
                        xml_char_o.set("layout", "EOL")
                        eol = False
                    if eoh == True:
                        xml_char_o.set("layout", "EOH")
                        eoh = False
        
        
        ###### <characters_target> 
        xml_target_chars = ET.SubElement(xml_token, "characters_target")
        target = target_word
        for j in range(0, len(target)):
            xml_l_char = ET.SubElement(xml_target_chars, "char_t", {"id":"t"+str(j+1)})
            xml_l_char.text = target[j]
            
        ##### <characters_aligned>
        if orig_word != None:
            xml_aligned_chars = ET.SubElement(xml_token, "characters_aligned")
            aligned_chars = my_python_levenshtein.align_levenshtein(orig_word, target_word)
            #aligned_chars = align(orig_word, target_word, with_n_m_alignment)
            #print(aligned_chars)
            num_aligned = 1
            for alignment in aligned_chars:
                xml_a_char = ET.SubElement(xml_aligned_chars, "char_a", {"id":"a"+str(num_aligned)})
                if not alignment[0] == ['<eps>', '<eps>']: 
                    if alignment[0][0] == alignment[0][1]:
                        xml_a_char.set("o_range", "o"+str(alignment[0][0]+1))
                    else:
                        xml_a_char.set("o_range", "o"+str(alignment[0][0]+1)+"..o"+str(alignment[0][1]+1))
                if not alignment[1] == ['<eps>', '<eps>']:
                    if alignment[1][0] == alignment[1][1]:
                        xml_a_char.set("t_range", "t"+str(alignment[1][0]+1))
                    else:
                        xml_a_char.set("t_range", "t"+str(alignment[1][0]+1)+"..t"+str(alignment[1][1]+1))    
                num_aligned += 1
        
        #### <phonemes_target>
        xml_target_phonemes = ET.SubElement(xml_token, "phonemes_target")
        
        phonemes = re.sub("\.", "", word[1]) #delete syllable boundaries
        phonemes = re.sub("'", "", phonemes) #delete stress marks
        
        phonemes = phonemes.split()
        
        target = word[0].split()
        target_start = 0
        phonemes_start = 0
        
        dont_annotate_phonemes = False #set to True if any of the exceptions below applies where no phoneme annotation is desired
        
        #no phoneme information for abbreviations (words ending with .), words without vowels (hmmmm) and words with at least 3 iterated characters (niiieee) (in a monomorphematic word -- otherwise <Betttuch> would not get phonemes)
        target_raw = "".join(target)
        target_raw = re.sub("_", "", target_raw)
        if target_raw.endswith("."): dont_annotate_phonemes = True 
        if not any(c.lower() in vowels for c in target_raw): dont_annotate_phonemes = True
        if re.search(r"(.)\1{3,}", target_raw.lower()): dont_annotate_phonemes = True
        elif re.search(r"(.)\1{2}", target_raw.lower()) and len(word[3].split()) == 1: dont_annotate_phonemes = True
        
        if not dont_annotate_phonemes == True:
        
            id = 1
            for j in range(len(phonemes)):
                phoneme = phonemes[j]
                if phoneme != "_": # no annotation if no phoneme present; punctuation marks are also filtered out this way (?? ? !', '_ _ _',)
                    xml_phon = ET.SubElement(xml_target_phonemes, "phon_t", {"id":"p"+str(id)})
                    if target[j] != "_" :
                        xml_phon.set("t_range", writexmlrange("t", [target_start, target_start+len(target[j])-1]) ) 
                        target_start += len(target[j])
                        phonemes_start += 1  
                    else:
                        phonemes_start += 1
                                    
                    xml_phon.text = phoneme
                    id += 1
                    
                else:
                    target_start += len(target[j])
                    continue
            
                   
        #### <phonemes_aligned>
        #xml_aligned_phonemes = ET.SubElement(xml_token, "phonemes_aligned")
        #target = word[0].split()
        #phonemes = phonemes #see phonemes_target
        #target_start = 0
        #phonemes_start = 0
        #id = 1
        #for j in range(len(target)):
        #    if target[j] == "_":
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "p_range":"p"+str(phonemes_start+1)})
        #        phonemes_start += 1
        #    elif phonemes[j] == "_":
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "t_range": writexmlrange("t", [target_start, target_start+len(target[j])-1])})
        #        target_start += len(target[j])
        #    else:
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "p_range":"p"+str(phonemes_start+1), "t_range": writexmlrange("t", [target_start, target_start+len(target[j])-1])})
        #        target_start += len(target[j])
        #        phonemes_start += 1
           
            
        ### <graphemes>
        xml_graphemes = ET.SubElement(xml_token, "graphemes_target")       
        target = word[0].split()
        num_graphemes = 1
        start = 1
        multi_graphs = ["ie", "ch", "sch", "qu"]#, "ng"]
    
        for j in range(len(target)):
            grapheme = target[j]
            if grapheme == "_":continue
            if grapheme.lower() in multi_graphs:
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)+".."+"t"+str(start+len(grapheme)-1)})
                xml_gra.set("type", grapheme)
                start += len(grapheme)
                num_graphemes += 1
            elif grapheme.lower() == "ieh":
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)+".."+"t"+str(start+1)})
                xml_gra.set("type", "ie")
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start+2)})
                start += len(grapheme)
                num_graphemes += 2
            else:
                for k in range(len(grapheme)):
                    xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)})
                    start += 1
                    num_graphemes += 1
                

        ###### <syllables>
        
        xml_syllables = ET.SubElement(xml_token, "syllables_target")
        
        if not dont_annotate_phonemes == True:
            phonemes = word[1].split()
            target = word[0].split()
            num_syll = 1
            start_syll = 1
            num_graphemes = 0 
            stressed = False
            reduced = False
            schaerfung_before = False
            schaerfung_after = False
            full_vowel = False #necessary to see if there is only a reduced vowel in the syllable or also a full vowel as in h 'aU s. t y: 6
            for j in range(0, len(phonemes)):
                if any(ph in ["a","e","i","o","u","y","9","2","E","I","O","U","Y"] for ph in phonemes[j]): full_vowel = True
                if target[j] != "_": 
                    num_graphemes += len(target[j])
                if "@" in phonemes[j] or ("6" in phonemes[j] and full_vowel == False): 
                    reduced = True
                if "'" in phonemes[j]:
                    stressed = True
                if "." in phonemes[j]:
                    if re.search(r"([bcdfgjklmnpqrstvwxz])\1", target[j]) or target[j] == "ck" or target[j] == "ng" or (target[j] == "z" and phonemes[j] == "t.s"):
                        if j == len(phonemes): schaerfung_before = False 
                        elif target[j+1][0] in consonants: schaerfung_before = False
                        elif j < len(phonemes) -1 and phonemes[j+1] == "?": schaerfung_before = False #edit 17.10.17 for sch l u ss _ e n d l i ch', S l U s. ? 'E n t. l I C (syllables: Schluss.endlich and not schlus.sendlich)
                        else: schaerfung_before = True 
                    elif j < len(phonemes)-1:
                        if re.search(r"([bcdfgjklmnpqrstvwxz])\1", target[j+1]) or target[j+1] == "ck" or target[j+1] == "ng":
                            num_graphemes += 1 #move syllable boundary one position to the right : gefal.len instead of  gefa.llen (see BAS output)
                            schaerfung_after = True
                    if schaerfung_before == True:
                        num_graphemes -= 1 #move syllable boundary one position to the left: fal.len instead of  fall.en and Dec.ke instead of  Deck.e
                    xml_syll = ET.SubElement(xml_syllables, "syll", {"id":"s"+str(num_syll), "range":"t"+str(start_syll)+".."+"t"+str(num_graphemes)})
                    if stressed == True:
                        xml_syll.set("type","stress")
                    elif reduced == True:
                        xml_syll.set("type","red")
                    else:
                        xml_syll.set("type", "unstress")
                    
                    num_syll += 1
                    start_syll = num_graphemes +1
                    if schaerfung_before == True: num_graphemes +=1
                    if schaerfung_after == True: num_graphemes -= 1
                    stressed = False
                    reduced = False
                    schaerfung_before = False
                    schaerfung_after = False
                    full_vowel = False
            xml_syll = ET.SubElement(xml_syllables, "syll", {"id":"s"+str(num_syll), "range":"t"+str(start_syll)+".."+"t"+str(num_graphemes)})
            if stressed == True:
                xml_syll.set("type","stress")
            elif reduced == True:
                xml_syll.set("type","red")
            else:
                xml_syll.set("type", "unstress")
            #if not re.sub("_", "", "".join(target)).isalpha(): #no syllable type for punctuation marks etc. -> edit: not necessary anymore because punctuation marks to not receive phonemes and syllables at all 
                #del xml_syll.attrib["type"]
            
        
           
        ###### <morphemes>
        xml_morphemes = ET.SubElement(xml_token, "morphemes_target")
        morphemes = word[3].split()
        morph_tags = word[4].split()
        morph_tags = [tag_morphemes.map_morphemetag(tag) for tag in morph_tags]
        pos_morphem = tag_morphemes.map_morphemetag(word[2]) # = STTS POS-tag mapped to morpheme tag (e.g. ADJD and ADJA are both mapped to ADJ)
        
        num_morphemes = 1
        start_morpheme = 1      
        
        #for monomorphematic words, the tag of the pos tagger is used
        if len(morphemes) == 1:
            xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m1", "range":"t1"+".."+"t"+str(len(morphemes[0]))})
            xml_mor.set("type", pos_morphem)
            
        #for words with two morphemes of which one is "INFL", use the POS Tag for the other morpheme
        elif len(morphemes) == 2 and any(morph == "INFL" for morph in morph_tags) and not (target_raw.istitle() and pos_morphem == "NN" and any(morph == "V" for morph in morph_tags)): #skip nominalized verbs:
            for j in range(0, len(morphemes)):
                xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m"+str(num_morphemes), "range":"t"+str(start_morpheme)+".."+"t"+str(start_morpheme + len(morphemes[j])-1)})
                if morph_tags[j] != "INFL":
                    xml_mor.set("type", pos_morphem)
                else:
                    xml_mor.set("type", morph_tags[j])
                
                start_morpheme += len(morphemes[j])
                num_morphemes += 1

            
        else:   
            for j in range(0, len(morphemes)):
                xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m"+str(num_morphemes), "range":"t"+str(start_morpheme)+".."+"t"+str(start_morpheme + len(morphemes[j])-1)})
                #if any(c.isalpha() for c in target_word): #no morpheme information for puncutation marks etc. (but punct. within words, e.g. hyphen, are included) -> edit: we also want morphemes for them
                xml_mor.set("type", morph_tags[j])
                    
                # old categorization
                """if morphemes[j] == "ge" and morph_tags[j] == "PRFX": #Korrektur von "ge"gangen -> kein Derivations- sondern Flexionspräfix
                    xml_mor.set("type", "infl_pref") 
                elif morphemes[j] == "en" and morph_tags[j] == "SFX": #Korrektur von gefall"en" -> kein Derivations- sondern Flexionssuffix
                    xml_mor.set("type", "infl_suf")
                elif morph_tags[j] in ["NN", "NE", "V", "ADJ", "ADV", "XY", "PIDAT", "CARD"]:
                    xml_mor.set("type","open")
                elif morph_tags[j] in ["ART", "PPER", "KON", "ADP", "APPRART", "PRF", "PPOSAT", "PPOS", "PTKZU", "PWS", "PTKNEG", "KOUS", "PIS" ]:
                    xml_mor.set("type", "closed")
                elif morph_tags[j] == "FG":
                    xml_mor.set("type", "inter")
                elif morph_tags[j] == "INFL":
                    if num_morphemes == 1:
                        xml_mor.set("type", "infl_pref")
                    else:
                        xml_mor.set("type","infl_suf")
                elif morph_tags[j] in ["PRFX", "PTKVZ"]:
                    xml_mor.set("type", "deriv_pref")
                elif morph_tags[j] == "SFX":
                    xml_mor.set("type", "deriv_suf")
                """
                    
                    
                start_morpheme += len(morphemes[j])
                num_morphemes += 1
        
        ### errors only prepare structure yet
        #ET.SubElement(xml_token, "errors")

    def prettify(elem): 
        """Return a pretty-printed XML string for the Element. http://stackoverflow.com/questions/17402323/use-xml-etree-elementtree-to-write-out-nicely-formatted-xml-files
        """
        rough_string = ET.tostring(elem, 'utf-8')
        reparsed = xml.dom.minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="\t")  
    
    print(prettify(xml_tokens), file=created_xml)
    #print(table)
    
    
    
def plaintolearnerxmlstring(csv_file=None,orig_file=None, target_file=None, word_list = None, xml_filename=None, bas_file = None, user_bas_file = None, input_type="list", with_n_m_alignment=False, meta_tokens = ["\h"], stanford_tag = True):
    
    metamarks = {} #for tilde and questionmark appearing in target
    
    if csv_file == None and orig_file == None and target_file == None and word_list == None:
        print("Error: No files were given.")
        return
    
    if csv_file == None and orig_file != None and target_file == None:
        print("Error: An orig but no target file was given.")
        return
    
    if word_list != None and (csv_file != None or orig_file != None or target_file != None):
        print("Error: Both a list and a file were given as input.")
        return
    
    #get 'clean' filename 
    if csv_file != None:
        filename = re.sub("(.+/)+", "",  csv_file) #delete path
    #elif target_file != None:
        #filename = re.sub("(.+/)+", "",  target_file) #delete path
    elif xml_filename != None:
        filename = re.sub("(.+/)+", "", xml_filename)
    else:
        print("You have to specify an xml_filename")
        return
    filename = re.sub("((_.+)+)?\.\w+", "",filename) #delete _anything_after_filename.txt
    
    
    #handle annotations ? and ~ in target (given as file)
    def clean_targetannotations(target_file):
        target=open(target_file, mode="r", encoding="utf-8").read().strip().splitlines()
        for i in range(len(target)):
            if target[i].startswith("?") and re.search("\w", target[i]):
                target[i] = target[i].lstrip("?")
                metamarks[i] = "?"
            
            if target[i].startswith("~"):
                target[i] = target[i].lstrip("~")
                metamarks[i] = "~"

        print("\n".join(target), file=open(target_file, mode="w", encoding="utf-8"))
        
         
    # Marie: not entered        
    #if orig and target given in separate files
    if csv_file == None and orig_file != None and target_file != None:
        orig = open(orig_file, mode="r", encoding="utf-8").read().strip().splitlines()
        clean_targetannotations(target_file)
        bas_input = target_file
        
    #if orig and target given in a csv-file    
    elif csv_file != None:
        origIN = open(csv_file, mode="r", encoding="utf-8").read().strip().splitlines()
        try:
            orig = [line.split("\t")[0] for line in origIN]
            target = [line.split("\t")[1] for line in origIN]
        except: 
            try:
                orig = [line.split(";")[0] for line in origIN]
                target = [line.split(";")[1] for line in origIN]       
            except:
                print("ERROR: Input file is not tab- or semicolon- separated") 
                return
                    
        print("\n".join(target), file=open("temporary.txt", mode= "w", encoding="utf-8"))
        clean_targetannotations("temporary.txt")
        bas_input = "temporary.txt"
        
    # Marie: not entered
    #if only target given:
    #elif csv_file == None and orig_file == None and target_file != None:
        #clean_targetannotations(target_file)
        #bas_input = target_file
        #orig = None

    # Marie: not entered
    #if input is given as list (word_list)
    #elif word_list != None and csv_file == None and orig_file == None and target_file == None:
        #if all(len(line) == 2 for line in word_list):
            #orig = [line[0] for line in word_list]
            #target = [line[1] for line in word_list]
        #else:
            #orig = None
            #target = word_list
        #print("\n".join(target), file=open("temporary.txt", mode= "w", encoding="utf-8"))
        #clean_targetannotations("temporary.txt")
        #bas_input = "temporary.txt"
        
       
        
    # Marie: not entered    
    #if BAS file is not given take information from the internet
    if bas_file == None:
        target = BAS_requests.getBAS(bas_input, input_type).strip() #handle semicolons that appeared in the target file
        target = re.sub(";;;XY;;;XY", "<semicolon>;;XY;<semicolon>;XY", target)  
        bas_dict = None

    else:

        words = open(bas_input, mode="r", encoding="utf-8").read().splitlines()
        target = []
        bas = open(bas_file, mode="r", encoding ="utf-8").read().strip().splitlines()
        
        # Marie: never entered
        #if user_bas_file != None:
            #user_bas_file = open(user_bas_file, mode="r+", encoding="utf-8")
            #user_bas = user_bas_file.read().strip().splitlines()
            #bas = bas + user_bas
        
        # Marie: 2s for full dict
        # make dictionary out of BAS list to speed up lookup
        # format: {"Dodo" : "Dodo;d ' o: . d o;NN;dodo;NN" }, target words are the keys and the complete BAS output as string the value
        bas_dict = {re.sub(" ", "", line.split(";")[0]):line for line in bas}
        
        for word in words:
            
            if word == "": #not target, e.g. in case of linebreak markers ^ which are only in the original
                target.append(";;;;")
                continue
            
            if word == ";":
                word = "<semicolon>"
               
            if word in bas_dict:
                target.append(bas_dict[word])
                
            #edit 18.1.18: annulled, because morphemes get different analyses e.g. for <fahrt> and <Fahrt>
            #edit 25.5.17: ignore titlecase (usually sentence initial) when looking up word 
            #we assume that titlecase/lowercase variant have the same properties, except for <weg>/<Weg>, <sucht>/<Sucht>,  but they are both included in the file 
            #thanks to Katrin for this code snippet
            #elif word.istitle() and word.lower() in bas_dict:
            #    bas_result = bas_dict[word.lower()]
            #    bas_result = bas_result[0].title() + bas_result[1:]
            #    target.append(bas_result) 
            #    bas_dict[word] = bas_result
                
            
            else:
                print("unknown:", word)
                bas_result = BAS_requests.getSingleBAS(word).strip()
                if bas_result == ";;;XY;;;XY":
                    bas_result = "<semicolon>;;XY;<semicolon>;XY" 
                target.append(bas_result)
                bas_dict[word] = bas_result #for quicker access if word appears more than once in the same text
                if user_bas_file != None: print(bas_result, file= user_bas_file) 
        
        # alter Abrufweg mit Listen, wahrscheinlich löschbar:
        
        #for i in range(len(words)):#
        #    found = False#
        #    for line_string in bas:#ausrücken
        #    #target.append(line_string) ####
        #        if re.sub(" ", "", line_string.split(";")[0]) == words[i]:#
        #            target.append(line_string)#
        #            found = True#
        #            break#  
        #                
        #    if found == False:# 
        #        print("unknown:", words[i])#
        #        bas_result = BAS_requests.getSingleBAS(words[i]) #
        #        print(bas_result)
        #        target.append(bas_result)#
        #        bas.append(bas_result) #for quicker access if word appears more than once
        #        if user_bas_file != None: print(bas_result, file= user_bas_file) #
                
                
        target = "\n".join(target)

    try: os.remove("temporary.txt")
    except: pass
    
    print("Done reading bas", datetime.datetime.now())

    # Organize BAS output in list
    table=[]

    # new: syllables are now in the main BAS file!! the syllable file has the form  "Dodo;d ' o: . d o" per line
    #if syllable_file != None:
    #    syllable_file = open(syllable_file, mode = "r", encoding = "utf-8").read().splitlines()    
        
    #    if user_syllable_file != None:
    #        user_syllable_file_handle = open(user_syllable_file, mode ="r+", encoding ="utf-8")
    #        user_syllable = user_syllable_file_handle.read().splitlines()
    #        syllable_file = syllable_file + user_syllable
    #        user_syllable_file_handle.close()
        
        # make dictionary out of syllable file of the form {Dodo:d'o:.do}
        #syllable_dictionary = {re.sub(" ", "", line.split(";")[0]):re.sub(" ", "", line.split(";")[1]) for line in syllable_file if len(line.split(";")) == 2}
    
    weights = LevenshteinWeights("g2pgewichte_ignorecase.xml", "xml") 
    aligner = LevenshteinAligner(weights)
    targets = target.split("\n")
    for i in range(len(targets)):
        
        #print(target.split("\n"))       
        line = targets[i].split(";") 
        #line = line.split(";")        
        
        if line[0] == "<semicolon>":
            line = [';', '','XY', ';', 'XY']
            table.append(line)
            continue
        
        if line == ['', '', '', '', '']: # if there is no target (e.g. in cases of linebreak markers ^ which are only in the original)
            table.append(line)
            continue
        
        if re.sub(" ", "", line[0]) in meta_tokens: # for us \h
            line = [re.sub(" ", "", line[0]), "", "", "", ""]
            table.append(line)
            continue
        
        #without syllable info, BAS output for <.> is [['.', '_', 'XY', '.', 'XY', '.']] but with syllable info  [['. _ _', "'< P >", 'XY', '.', 'XY', '.']] which makes problems
        if not any(c.isalpha() for c in line[0]):
            line[1] = ""
         
        #print(line)
           
        graphemes = re.sub(" ", "", line[0])
        phonemes = re.sub("\.", "", line[1]) #new: syllables and stress marks are included in the main BAS file: here they are removed to obtain phonemes only 
        phonemes = re.sub("'", "", phonemes)
        phonemes = re.sub(" ", "", phonemes)
        
        #print("start g2p align", datetime.datetime.now())
        # align graphemes and phonemes
        g2p_aligned_tuples = g2p.g2p_align_optimized(graphemes, phonemes, aligner)
        #print("g2p align done", datetime.datetime.now())
        #print(g2p_aligned_tuples)
        g2p_aligned_tuples = g2p.refine_g2p_align(g2p_aligned_tuples)
        #print(g2p_aligned_tuples)
        g2p_aligned_tuples_withsyllables = g2p.insert_syllables(g2p_aligned_tuples, bas_dictionary = bas_dict)#[0] # returns tuple ( (graphemes,phonemes), updated syllable dictionary)
        #print(g2p_aligned_tuples_withsyllables)
        
        graphemes = " ".join(g for (g,p) in g2p_aligned_tuples_withsyllables)
        phonemes_with_syllables = " ".join(p for (g,p) in g2p_aligned_tuples_withsyllables)
        
        graphemes = re.sub("<eps>", "_", graphemes)
        phonemes_with_syllables = re.sub("<eps>", "_", phonemes_with_syllables)
        
        table.append([graphemes, phonemes_with_syllables, line[2], line[3], line[4]])       
        
    # Append original xmltoken to BAS table if original xmltoken were given
    if orig != None:    
        try:
            for i in range(len(orig)):
                table[i].append(orig[i])
        except:
            print("Error: Number of original tokens and number of target tokens is not the same!")
  
    if stanford_tag == True:
        #POS-Tags: substitute those of BAS with those of tagger
        targets = [re.sub("\s|_", "", elem[0]) for elem in table] #attention: ^ is not included in this list because target is empty ""
        
        # POS-Tagging with Stanford-Tagger (currently not working)
        # tagged_targets = tagger.tag_list(targets, tagger_type="kids", correct_format=True) #output: [('Das', 'PDS'), ('ist', 'VAFIN'), ('ein', 'ART'), ('einfacher', 'ADJA'), ('Test', 'NN'), ('zum', 'APPRART'), ('\\h', 'XY'), ('Ausprobieren', 'NN')]

        # Workaround: filling tagged_target list with dummy POS-Tags
        # output: [('Das', 'NA'), ('ist', 'NA')....
        tagged_targets = []

        for i, target in enumerate(targets):
            item = "('" + target + "', 'NA')"
            tagged_targets.append(item)


        pos_list = [elem[1] for elem in tagged_targets] #['PDS', 'VAFIN', 'ART', 'ADJA', 'NN', 'APPRART', 'XY', 'NN']
    
        table_counter = 0
        pos_counter = 0
        while table_counter < len(table):
            if table[table_counter][0] != "": # since ^ has no target, this is skipped
                table[table_counter][2] = pos_list[pos_counter]
                table_counter += 1
                pos_counter += 1
            else:
                table_counter += 1
        
    #table_backup = open("table_backup.txt", mode="w", encoding="utf-8")
    #print(table, file=table_backup)
    
    ################
    #table = ast.literal_eval(open("table_backup.txt", mode="r", encoding="utf-8").read())
    ################
    
    # Create BAS output file and print BAS output to separate file
    #if bas_file == None:
    #    bas_output = open(filename+"_bas.txt", mode="w", encoding="utf-8") 
    #    print(target, file = bas_output)
       
    # Create xml output file
    if xml_filename == None: created_xml = open(filename+".xml", mode="w")
    else: created_xml = open(xml_filename, mode="w")
    
    # word = table[i] --> each element represents information for one word
    # word[0] = target-orthography
    # word[1] = target-sampa (word[0] and word[1] are aligned; spaces mark boundaries of corresponding characters ['h a ll o', "h 'a. l o:"]
    # word[2] = target POS-tag
    # word[3] = target morphemes
    # word[4] = target morpheme POS-tags
    # (word[5] = original orthography if given)
    
    print("writing output file", datetime.datetime.now()) 
    #Create xml root
    xml_tokens = ET.Element("tokens", {"id":filename})
    
    tok_counter = 1
    
    childlex_lexicon_lower = ""
    with open("childlex/childlex_0.17.01_litkey_types.txt", mode="r", encoding="utf-8") as childlex_lexicon:
        childlex_lexicon = set(childlex_lexicon.read().splitlines())
        childlex_lexicon_lower = set([c.lower() for c in childlex_lexicon])
    
    for i in range(0,len(table)):
        #print("start processing table item", datetime.datetime.now())  
        
        word = table[i]
                
        #Alert if there are more graphemes than phonemes or vice versa
        if len(word[0].split()) != len(word[1].split()) and word[0] not in metamarks and word[0] != "\\h" and word[0] !=";":
            print(word[0] + " and " + word[1] +" are not aligned correctly!")
        
        if orig != None:     
            orig_word = re.sub("\^", "", word[5])
        else: orig_word = None
        
        # remove blanks in the target graphemes (BAS splits all characters)    
        target_word = re.sub("\s", "", word[0])
        target_word = re.sub("_", "", target_word)
        
        #store where the next token is ^ (end of line) or \h (end of headline)
        eol = False
        eoh = False
        if i < len(table)-1:
            if orig != None:
                next_orig = table[i+1][5]
                if next_orig == "^":
                    eol = True
            next_target = table[i+1][0]
            if next_target =="\h":
                eoh = True
            
        #print("start xml token", datetime.datetime.now())  
        
        ###### <xmltoken> 
        if target_word in meta_tokens or target_word == "": # \h, ^ are ignored
            continue
        xml_token = ET.SubElement(xml_tokens, "token", {"id":"tok"+str(tok_counter), "target": target_word})
        tok_counter += 1
        if orig_word != None:
            xml_token.set("orig", orig_word) #delete ^ within orig word
        if i in metamarks:
            if metamarks[i] == "~":
                xml_token.set("target_comments", "ungram")
            elif metamarks[i] == "?":
                xml_token.set("target_comments", "unclear/onom")
        
        #print("start POS", datetime.datetime.now())  
        
        #### POS
        xml_token.set("pos_stts", word[2])
        
        #print("start realword", datetime.datetime.now())  
        
        ##### realword
        if orig_word != None and orig_word != target_word:
            
            #remove linebreak marks and hyphens at linebreaks (these are ignored when looking up the word in childlex)
            orig_word_transformed = re.sub(r"-\^", "", orig_word)
            orig_word_transformed = re.sub(r"\^-", "", orig_word_transformed)
            orig_word_transformed = re.sub(r"\^", "", orig_word_transformed)
            
            #the first token with a split mark is concatenated with the subsequent token(s) and this concatenation is evaluated
            # e.g. "auf| jeden| fall: the first token is analyzed as "aufjedenfall" but the others are treated separately ("jeden" and "fall") 
            if "|" in orig_word_transformed and i < len(table) and orig != None:
                if not (i > 0 and "|" in table[i-1][5]): #if the preceeding word already had a split mark, no concatenation takes place but only this word is analyzed
                    n = 1
                    orig_word_transformed = orig_word_transformed + table[i+n][5]
                    while "|" in table[i+n][5] and i+n < len(table): #collect all parts of the word that were written together
                        orig_word_transformed = orig_word_transformed + table[i+n+1][5]
                        n += 1
                    
            orig_word_transformed = re.sub(r"\|", "", orig_word_transformed)
            #print(orig_word, orig_word_transformed)
            
            exist_orig = all(part in childlex_lexicon_lower for part in re.split(r"[_-]", orig_word_transformed.lower())) #split word at - or _ and see if all parts exist in childlex
            if exist_orig: xml_token.set("realword", str(exist_orig).lower()) #only include when "True"
            
        #print("start characters_orig", datetime.datetime.now())  
        
        ###### <characters_orig> 
        if orig_word != None:
            xml_chars_orig = ET.SubElement(xml_token, "characters_orig")
            orig = word[5] #again including ^
            o_counter = 1
            
            for j in range(0, len(orig)):   
                
                if orig[j] == "^":
                    continue
                          
                xml_char_o = ET.SubElement(xml_chars_orig, "char_o", {"id":"o"+str(o_counter)})
                o_counter += 1
                xml_char_o.text = orig[j]
                
                if j < len(orig)-1 and orig[j+1] == "^":
                    xml_char_o.set("layout", "EOL")
                
                if j == len(orig)-1: #set end of line/headline at last character if applicable
                    if eol == True: 
                        xml_char_o.set("layout", "EOL")
                        eol = False
                    if eoh == True:
                        xml_char_o.set("layout", "EOH")
                        eoh = False
        
        #print("start characters_target", datetime.datetime.now())  
        
        ###### <characters_target> 
        xml_target_chars = ET.SubElement(xml_token, "characters_target")
        target = target_word
        for j in range(0, len(target)):
            xml_l_char = ET.SubElement(xml_target_chars, "char_t", {"id":"t"+str(j+1)})
            xml_l_char.text = target[j]
            
        #print("start characters_aligned", datetime.datetime.now())  
            
        ##### <characters_aligned>
        if orig_word != None:
            xml_aligned_chars = ET.SubElement(xml_token, "characters_aligned")
            aligned_chars = my_python_levenshtein.align_levenshtein(orig_word, target_word)
            #aligned_chars = align(orig_word, target_word, with_n_m_alignment)
            #print(aligned_chars)
            num_aligned = 1
            for alignment in aligned_chars:
                xml_a_char = ET.SubElement(xml_aligned_chars, "char_a", {"id":"a"+str(num_aligned)})
                if not alignment[0] == ['<eps>', '<eps>']: 
                    if alignment[0][0] == alignment[0][1]:
                        xml_a_char.set("o_range", "o"+str(alignment[0][0]+1))
                    else:
                        xml_a_char.set("o_range", "o"+str(alignment[0][0]+1)+"..o"+str(alignment[0][1]+1))
                if not alignment[1] == ['<eps>', '<eps>']:
                    if alignment[1][0] == alignment[1][1]:
                        xml_a_char.set("t_range", "t"+str(alignment[1][0]+1))
                    else:
                        xml_a_char.set("t_range", "t"+str(alignment[1][0]+1)+"..t"+str(alignment[1][1]+1))    
                num_aligned += 1
        
        #print("start phonemes_target", datetime.datetime.now())  
        
        #### <phonemes_target>
        xml_target_phonemes = ET.SubElement(xml_token, "phonemes_target")
        
        phonemes = re.sub("\.", "", word[1]) #delete syllable boundaries
        phonemes = re.sub("'", "", phonemes) #delete stress marks
        
        phonemes = phonemes.split()
        
        target = word[0].split()
        target_start = 0
        phonemes_start = 0
        
        dont_annotate_phonemes = False #set to True if any of the exceptions below applies where no phoneme annotation is desired
        
        #no phoneme information for abbreviations (words ending with .), words without vowels (hmmmm) and words with at least 3 iterated characters (niiieee) (in a monomorphematic word -- otherwise <Betttuch> would not get phonemes)
        target_raw = "".join(target)
        target_raw = re.sub("_", "", target_raw)
        if target_raw.endswith("."): dont_annotate_phonemes = True 
        if not any(c.lower() in vowels for c in target_raw): dont_annotate_phonemes = True
        if re.search(r"(.)\1{3,}", target_raw.lower()): dont_annotate_phonemes = True
        elif re.search(r"(.)\1{2}", target_raw.lower()) and len(word[3].split()) == 1: dont_annotate_phonemes = True
        
        if not dont_annotate_phonemes == True:
        
            id = 1
            for j in range(len(phonemes)):
                phoneme = phonemes[j]
                if phoneme != "_": # no annotation if no phoneme present; punctuation marks are also filtered out this way (?? ? !', '_ _ _',)
                    xml_phon = ET.SubElement(xml_target_phonemes, "phon_t", {"id":"p"+str(id)})
                    if target[j] != "_" :
                        xml_phon.set("t_range", writexmlrange("t", [target_start, target_start+len(target[j])-1]) ) 
                        target_start += len(target[j])
                        phonemes_start += 1  
                    else:
                        phonemes_start += 1
                                    
                    xml_phon.text = phoneme
                    id += 1
                    
                else:
                    target_start += len(target[j])
                    continue
            
                   
        #### <phonemes_aligned>
        #xml_aligned_phonemes = ET.SubElement(xml_token, "phonemes_aligned")
        #target = word[0].split()
        #phonemes = phonemes #see phonemes_target
        #target_start = 0
        #phonemes_start = 0
        #id = 1
        #for j in range(len(target)):
        #    if target[j] == "_":
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "p_range":"p"+str(phonemes_start+1)})
        #        phonemes_start += 1
        #    elif phonemes[j] == "_":
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "t_range": writexmlrange("t", [target_start, target_start+len(target[j])-1])})
        #        target_start += len(target[j])
        #    else:
        #        xml_a_phon = ET.SubElement(xml_aligned_phonemes, "phon_a", {"id": "pa"+str(j+1), "p_range":"p"+str(phonemes_start+1), "t_range": writexmlrange("t", [target_start, target_start+len(target[j])-1])})
        #        target_start += len(target[j])
        #        phonemes_start += 1
           
        #print("start graphemes", datetime.datetime.now())  
            
        ### <graphemes>
        xml_graphemes = ET.SubElement(xml_token, "graphemes_target")       
        target = word[0].split()
        num_graphemes = 1
        start = 1
        multi_graphs = ["ie", "ch", "sch", "qu"]#, "ng"]
    
        for j in range(len(target)):
            grapheme = target[j]
            if grapheme == "_":continue
            if grapheme.lower() in multi_graphs:
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)+".."+"t"+str(start+len(grapheme)-1)})
                xml_gra.set("type", grapheme)
                start += len(grapheme)
                num_graphemes += 1
            elif grapheme.lower() == "ieh":
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)+".."+"t"+str(start+1)})
                xml_gra.set("type", "ie")
                xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start+2)})
                start += len(grapheme)
                num_graphemes += 2
            else:
                for k in range(len(grapheme)):
                    xml_gra = ET.SubElement(xml_graphemes, "gra", {"id":"g"+str(num_graphemes), "range":"t"+str(start)})
                    start += 1
                    num_graphemes += 1
        
        #print("start syllables", datetime.datetime.now())  
                
        ###### <syllables>
        
        xml_syllables = ET.SubElement(xml_token, "syllables_target")
        
        if not dont_annotate_phonemes == True:
            phonemes = word[1].split()
            target = word[0].split()
            num_syll = 1
            start_syll = 1
            num_graphemes = 0 
            stressed = False
            reduced = False
            schaerfung_before = False
            schaerfung_after = False
            full_vowel = False #necessary to see if there is only a reduced vowel in the syllable or also a full vowel as in h 'aU s. t y: 6
            for j in range(0, len(phonemes)):
                if any(ph in ["a","e","i","o","u","y","9","2","E","I","O","U","Y"] for ph in phonemes[j]): full_vowel = True
                if target[j] != "_": 
                    num_graphemes += len(target[j])
                if "@" in phonemes[j] or ("6" in phonemes[j] and full_vowel == False): 
                    reduced = True
                if "'" in phonemes[j]:
                    stressed = True
                if "." in phonemes[j]:
                    if re.search(r"([bcdfgjklmnpqrstvwxz])\1", target[j]) or target[j] == "ck" or target[j] == "ng" or (target[j] == "z" and phonemes[j] == "t.s"):
                        if j == len(phonemes): schaerfung_before = False 
                        elif target[j+1][0] in consonants: schaerfung_before = False
                        elif j < len(phonemes) -1 and phonemes[j+1] == "?": schaerfung_before = False #edit 17.10.17 for sch l u ss _ e n d l i ch', S l U s. ? 'E n t. l I C (syllables: Schluss.endlich and not schlus.sendlich)
                        else: schaerfung_before = True 
                    elif j < len(phonemes)-1:
                        if re.search(r"([bcdfgjklmnpqrstvwxz])\1", target[j+1]) or target[j+1] == "ck" or target[j+1] == "ng":
                            num_graphemes += 1 #move syllable boundary one position to the right : gefal.len instead of  gefa.llen (see BAS output)
                            schaerfung_after = True
                    if schaerfung_before == True:
                        num_graphemes -= 1 #move syllable boundary one position to the left: fal.len instead of  fall.en and Dec.ke instead of  Deck.e
                    xml_syll = ET.SubElement(xml_syllables, "syll", {"id":"s"+str(num_syll), "range":"t"+str(start_syll)+".."+"t"+str(num_graphemes)})
                    if stressed == True:
                        xml_syll.set("type","stress")
                    elif reduced == True:
                        xml_syll.set("type","red")
                    else:
                        xml_syll.set("type", "unstress")
                    
                    num_syll += 1
                    start_syll = num_graphemes +1
                    if schaerfung_before == True: num_graphemes +=1
                    if schaerfung_after == True: num_graphemes -= 1
                    stressed = False
                    reduced = False
                    schaerfung_before = False
                    schaerfung_after = False
                    full_vowel = False
            xml_syll = ET.SubElement(xml_syllables, "syll", {"id":"s"+str(num_syll), "range":"t"+str(start_syll)+".."+"t"+str(num_graphemes)})
            if stressed == True:
                xml_syll.set("type","stress")
            elif reduced == True:
                xml_syll.set("type","red")
            else:
                xml_syll.set("type", "unstress")
            #if not re.sub("_", "", "".join(target)).isalpha(): #no syllable type for punctuation marks etc. -> edit: not necessary anymore because punctuation marks to not receive phonemes and syllables at all 
                #del xml_syll.attrib["type"]
            
        #print("start morphemes", datetime.datetime.now())  
           
        ###### <morphemes>
        xml_morphemes = ET.SubElement(xml_token, "morphemes_target")
        morphemes = word[3].split()
        morph_tags = word[4].split()
        morph_tags = [tag_morphemes.map_morphemetag(tag) for tag in morph_tags]
        pos_morphem = tag_morphemes.map_morphemetag(word[2]) # = STTS POS-tag mapped to morpheme tag (e.g. ADJD and ADJA are both mapped to ADJ)
        
        num_morphemes = 1
        start_morpheme = 1      
        
        #for monomorphematic words, the tag of the pos tagger is used
        if len(morphemes) == 1:
            xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m1", "range":"t1"+".."+"t"+str(len(morphemes[0]))})
            xml_mor.set("type", pos_morphem)
            
        #for words with two morphemes of which one is "INFL", use the POS Tag for the other morpheme
        elif len(morphemes) == 2 and any(morph == "INFL" for morph in morph_tags) and not (target_raw.istitle() and pos_morphem == "NN" and any(morph == "V" for morph in morph_tags)): #skip nominalized verbs:
            for j in range(0, len(morphemes)):
                xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m"+str(num_morphemes), "range":"t"+str(start_morpheme)+".."+"t"+str(start_morpheme + len(morphemes[j])-1)})
                if morph_tags[j] != "INFL":
                    xml_mor.set("type", pos_morphem)
                else:
                    xml_mor.set("type", morph_tags[j])
                
                start_morpheme += len(morphemes[j])
                num_morphemes += 1

            
        else:   
            for j in range(0, len(morphemes)):
                xml_mor = ET.SubElement(xml_morphemes, "mor", {"id":"m"+str(num_morphemes), "range":"t"+str(start_morpheme)+".."+"t"+str(start_morpheme + len(morphemes[j])-1)})
                #if any(c.isalpha() for c in target_word): #no morpheme information for puncutation marks etc. (but punct. within words, e.g. hyphen, are included) -> edit: we also want morphemes for them
                xml_mor.set("type", morph_tags[j])
                    
                # old categorization
                """if morphemes[j] == "ge" and morph_tags[j] == "PRFX": #Korrektur von "ge"gangen -> kein Derivations- sondern Flexionspräfix
                    xml_mor.set("type", "infl_pref") 
                elif morphemes[j] == "en" and morph_tags[j] == "SFX": #Korrektur von gefall"en" -> kein Derivations- sondern Flexionssuffix
                    xml_mor.set("type", "infl_suf")
                elif morph_tags[j] in ["NN", "NE", "V", "ADJ", "ADV", "XY", "PIDAT", "CARD"]:
                    xml_mor.set("type","open")
                elif morph_tags[j] in ["ART", "PPER", "KON", "ADP", "APPRART", "PRF", "PPOSAT", "PPOS", "PTKZU", "PWS", "PTKNEG", "KOUS", "PIS" ]:
                    xml_mor.set("type", "closed")
                elif morph_tags[j] == "FG":
                    xml_mor.set("type", "inter")
                elif morph_tags[j] == "INFL":
                    if num_morphemes == 1:
                        xml_mor.set("type", "infl_pref")
                    else:
                        xml_mor.set("type","infl_suf")
                elif morph_tags[j] in ["PRFX", "PTKVZ"]:
                    xml_mor.set("type", "deriv_pref")
                elif morph_tags[j] == "SFX":
                    xml_mor.set("type", "deriv_suf")
                """
                    
                    
                start_morpheme += len(morphemes[j])
                num_morphemes += 1
        
        ### errors only prepare structure yet
        #ET.SubElement(xml_token, "errors")
    print("done writing output file", datetime.datetime.now())  

    def prettify(elem): 
        """Return a pretty-printed XML string for the Element. http://stackoverflow.com/questions/17402323/use-xml-etree-elementtree-to-write-out-nicely-formatted-xml-files
        """
        rough_string = ET.tostring(elem, 'utf-8')
        reparsed = xml.dom.minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="\t")  
    
    #print(prettify(xml_tokens), file=created_xml)
    #print(table)
    return prettify(xml_tokens)
    
    
############################################################################################################################################################

if __name__ == "__main__":
    
    functionality = """This script produces a LearnerXML file without spelling errors for either pairs of original and target tokens or target tokens only. (Useful if errors are supposed to 
    be annotated manually)."""
    
    parser = argparse.ArgumentParser(description=functionality)
    parser.add_argument('infile', nargs=1, help='the input file; default: csv-file with original token left and target token right separated by a tab (one pair per line). You can also specify a directory here and all files in this directory will be processed and the results stored in a folder named "xml" (in this case the outfile name will always be <infile>.xml')
    parser.add_argument("--targetonly", action="store_true", default=False, help="use this if the input only contains target tokens")
    parser.add_argument('-o', '--outfile', help='specify output file; if not given, the output file will be <infile>.xml')
    parser.add_argument('--bas',  help='specify exttab output from BAS web service to use offline to accelerate the procedure; if not given, it will be tried to use childlex/cd_min0.02.g2p.tab')
    parser.add_argument('--user_bas', help='specify a file in which words that are not yet listed in the main BAS file are stored for later use (default: user_dict/user_bas.g2p.tab)')
    parser.add_argument("-m", "--metatokens", nargs="*", help="metatokens to ignore for all layers except for 'tokens_orig' and 'tokens_target'")
    
    args = parser.parse_args()
           
    
    #input: target words vs. orig and target words
    if args.targetonly:
        a_csv_file = None
        a_target_file = args.infile[0]
    else:
        a_target_file = None
        a_csv_file = args.infile[0]
        
    #BAS-input file for offline use    
    if args.bas:
        try:
            open(args.bas, mode="r", encoding="utf-8")
            a_bas_file = args.bas
        except:
            a_bas_file = None
            print("The BAS-file you specified does not exist, so I'm trying to use the default")
            try:
                open("childlex/childlex_0.17.01_litkey_types.g2p.tab", mode="r", encoding="utf-8")
                a_bas_file = "childlex/childlex_0.17.01_litkey_types.g2p.tab"
            except:
                a_bas_file = None
                print("Error: The default BAS-file (childlex/childlex_0.17.01_litkey_types.g2p.tab) could not be loaded")
    else:
        try:
            open("childlex/childlex_0.17.01_litkey_types.g2p.tab", mode="r", encoding="utf-8")
            a_bas_file = "childlex/childlex_0.17.01_litkey_types.g2p.tab"
        except:
            a_bas_file = None
            print("Attention: The default BAS-file (childlex/childlex_0.17.01_litkey_types.g2p.tab) could not be loaded")
            
    #user BAS file
    if args.user_bas:
        try:
            open(args.user_bas, mode="r", encoding="utf-8")
        except:
            print("The user BAS file you specified does not exist, so I'm trying to use the default (user_dict/user_bas.g2p.tab)")
            try:
                open("user_dict/user_bas.g2p.tab", mode="r", encoding="utf-8")
                args.user_bas = "user_dict/user_bas.g2p.tab"
            except:
                print("Error: The default user BAS-file (user_dict/user_bas.g2p.tab) could not be loaded")
    else:
        try:
            open("user_dict/user_bas.g2p.tab", mode="r", encoding="utf-8")
            args.user_bas = "user_dict/user_bas.g2p.tab"
        except:
            print("Attention: The default user BAS-file (user_dict/user_bas.g2p.tab) could not be loaded")
    
    
    #Syllable input file for offline use       
    #if args.syll:
    #    try:
    #        open(args.syll, mode="r", encoding="utf-8")
    #        a_syll_file = args.syll
    #    except:
    #        a_syll_file = None
    #        print("The syllable file you specified does not exist, so I'm trying to use the default")
    #        try:
    #            open("childlex/cd_min0.02_syll.g2p.tab", mode="r", encoding="utf-8")
    #            a_syll_file = "childlex/cd_min0.02_syll.g2p.tab"
    #        except:
    #            a_syll_file = None
    #            print("Error: The default syllable file (childlex/cd_min0.02_syll.g2p.tab) could not be loaded")
    #else:
    #    try:
    #        open("childlex/cd_min0.02_syll.g2p.tab", mode="r", encoding="utf-8")
    #        a_syll_file = "childlex/cd_min0.02_syll.g2p.tab"
    #    except:
    #        a_syll_file = None
    #        print("Attention: The default syllable file (childlex/cd_min0.02_syll.g2p.tab) could not be loaded")
            
    
    #User syllable file
    #if args.user_syll:
    #    try:
    #        open(args.user_syll, mode="r", encoding="utf-8")
    #    except:
    #        print("The user syllable file you specified does not exist, so I'm trying to use the default (user_dict/user_syll.g2p.tab)")
    #        try:
    #            open("user_dict/user_syll.g2p.tab", mode="r", encoding="utf-8")
    #            args.user_syll = "user_dict/user_syll.g2p.tab"
    #        except:
    #            print("Error: The default user syllable file (user_dict/user_syll.g2p.tab) could not be loaded")
    #else:
    #    try:
    #        open("user_dict/user_syll.g2p.tab", mode="r", encoding="utf-8")
    #        args.user_syll = "user_dict/user_syll.g2p.tab"
    #    except:
    #        print("Attention: The default user syllable file (user_dict/user_syll.g2p.tab) could not be loaded")
    
    
    #Metatokens
    if not args.metatokens:
        args.metatokens = []
        
    
    if os.path.isdir(args.infile[0]):
        for file in os.listdir(path=args.infile[0]):
            if a_csv_file != None: a_csv_file = args.infile[0]+"/"+file
            elif a_target_file != None: a_target_file = args.infile[0]+"/"+file
            args.outfile = re.sub("\..+$", ".xml", file)
            plaintolearnerxml(csv_file=a_csv_file, target_file = a_target_file, xml_filename="xml/"+args.outfile, bas_file=a_bas_file, user_bas_file=args.user_bas, meta_tokens=args.metatokens)

    else:
          
        plaintolearnerxml(csv_file=a_csv_file, target_file = a_target_file, xml_filename=args.outfile, bas_file=a_bas_file, user_bas_file=args.user_bas, meta_tokens=args.metatokens)

