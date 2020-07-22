#coding=utf-8
'''
Created on 22.07.2015

Functions for the alignment of graphemes and phonemes and insertion of syllable information.

@author: Ronja Laarmann-Quante
'''
import re, os
import marcel_levenshtein.leven_align
import requests
import xml.etree.ElementTree as ET

import datetime


#alignment_output = open("alignment_output.txt", mode="w", encoding="utf-8")
#test = open("test.txt", mode='r',  encoding="utf-8").read()
vowels = ["a", "e", "i", "o", "u", "ä", "ö", "ü"]

def getBAS_g2p(infile):
    #upload text to BAS webservice G2P
    url = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/services/runG2P"
    file = {"i": open(infile)}
    postdata = {"com":"no",
                "align":"no",
                "stress":"no", 
                "lng":"deu-DE",
                "syl":"no", 
                "embed":"no",
                "iform": "list", 
                "nrm":"no", 
                "oform":"tab",
                "map":"no",
                "featset":"extended",
                "tgrate":"16000",
                "tgitem":"ort"}
    r = requests.post(url, data=postdata, files=file)
    
    #download result file
    root = ET.fromstring(r.text)
    downloadLink = root.find("downloadLink").text
    
    result = requests.get(downloadLink)
    result.encoding = "utf-8"
    
    return result.text

def getBAS_g2p_syllables(infile):
    #upload text to BAS webservice G2P
    url = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/services/runG2P"
    file = {"i": open(infile)}
    postdata = {"com":"no",
                "align":"no",
                "stress":"yes", 
                "lng":"deu-DE",
                "syl":"yes", 
                "embed":"no",
                "iform": "list", 
                "nrm":"no",
                "oform":"tab",
                "map":"no", 
                "featset":"extended",
                "tgrate":"16000",
                "tgitem":"ort"}
    r = requests.post(url, data=postdata, files=file)
    
    #download result file
    root = ET.fromstring(r.text)
    downloadLink = root.find("downloadLink").text
    
    result = requests.get(downloadLink)
    result.encoding = "utf-8"
    
    return result.text

def getSingleBAS_g2p(single_word):
    print(single_word, file=open("temporary.txt", mode= "w", encoding="utf-8"))
    target = getBAS_g2p("temporary.txt")
    os.remove("temporary.txt")
    return target

def getSingleBAS_g2p_syllables(single_word):
    print(single_word, file=open("temporary.txt", mode= "w", encoding="utf-8"))
    target = getBAS_g2p_syllables("temporary.txt")
    os.remove("temporary.txt")
    return target

def g2p_align_optimized(graphemes, phonemes, aligner):
    
    #edit 30.11.: these are no longer repaired within the program but externally
    #repair wrong BAS output that causes errors in the further processing (when converting to EXMARaLDA) --> also repair in insert_syllables
    #print(graphemes, phonemes)
    #if graphemes == "Fr." and phonemes == "?Ef?Er": phonemes = "fr"
    #if graphemes == "antworteten" and phonemes == "?antvO6t@?Et@n": phonemes = "?antvO6t@t@n"
    #if graphemes == "verbindete" and phonemes == "fE6b@?Ind@t@": phonemes = "fE6bInd@t@"

    #aligned = marcel_levenshtein.leven_align.get_alignment(graphemes.lower(), phonemes, param)
    aligned = aligner.bio_align(graphemes.lower(),phonemes)
    #aligned = marcel_levenshtein.leven_align.MainApplication(graphemes.lower(), phonemes, param).run()
    #alignment is trained on lowercase letters so all graphemes are first converted to lowercase
    #print(aligned)
    aligned = aligned[0] #only first alignment version if there are several
    aligned = [list(a) for a in aligned]
    
    #then graphemes are mapped back to their original letter case
    a = 0 #alignment unit index
    g = 0 #grapheme index
    
    while a < len(aligned):
        if aligned[a][0] != '<eps>':
            if graphemes[g].isupper():
                aligned[a][0] = aligned[a][0].upper()
            g += 1
            a += 1
        else:
            a += 1
    aligned = [tuple(a) for a in aligned]
    #print("unrefined", aligned)
    return aligned

def g2p_align(graphemes, phonemes, param = "g2pgewichte_ignorecase.xml"):
    
    #edit 30.11.: these are no longer repaired within the program but externally
    #repair wrong BAS output that causes errors in the further processing (when converting to EXMARaLDA) --> also repair in insert_syllables
    #print(graphemes, phonemes)
    #if graphemes == "Fr." and phonemes == "?Ef?Er": phonemes = "fr"
    #if graphemes == "antworteten" and phonemes == "?antvO6t@?Et@n": phonemes = "?antvO6t@t@n"
    #if graphemes == "verbindete" and phonemes == "fE6b@?Ind@t@": phonemes = "fE6bInd@t@"

    aligned = marcel_levenshtein.leven_align.get_alignment(graphemes.lower(), phonemes, param)
    #aligned = marcel_levenshtein.leven_align.MainApplication(graphemes.lower(), phonemes, param).run()
    #alignment is trained on lowercase letters so all graphemes are first converted to lowercase
    #print(aligned)
    aligned = aligned[0] #only first alignment version if there are several
    aligned = [list(a) for a in aligned]
    
    #then graphemes are mapped back to their original letter case
    a = 0 #alignment unit index
    g = 0 #grapheme index
    
    while a < len(aligned):
        if aligned[a][0] != '<eps>':
            if graphemes[g].isupper():
                aligned[a][0] = aligned[a][0].upper()
            g += 1
            a += 1
        else:
            a += 1
    aligned = [tuple(a) for a in aligned]
    #print("unrefined", aligned)
    return aligned

#input: List of aligned tuples
#output: List of aligned tuples with n:m mappings
def refine_g2p_align(aligned_g2p, document_errors=True):   
    
    #exception_file = open("childlex/bas_exceptions_autocorrect.txt", mode="r+", encoding="utf-8")
     
    aligned = aligned_g2p
    eps = "<eps>"
    for i in range(len(aligned)): 
        aligned[i] = list(aligned[i])
        g = aligned[i][0] #grapheme
        p = aligned[i][1] #phoneme
        
        if i < len(aligned)-1:
            aligned[i+1] = list(aligned[i+1])
            next_g = aligned[i+1][0] #right neighbor grapheme
            next_p = aligned[i+1][1] #right neighbor phoneme
            
            if not aligned[i] == [eps, eps]:
                
                # correct wrong alignments with glottal stop such as ('E', '?'), ('<eps>', 'a'), ('i', 'I'), ('s', 's') to ('<eps>', '?'), ('E', 'a')
                if p == "?" and g != eps and next_g == eps:
                    aligned[i+1][0] = g
                    aligned[i][0] = eps
                
                
                #long vowels
                if i < len(aligned)-2:
                    aligned[i+2] = list(aligned[i+2])
                    second_next_g = aligned[i+2][0]
                    second_next_p = aligned[i+2][1]  
                    
                    if g.lower() == "i" and next_g.lower() =="e" and second_next_g.lower() == "h":
                        aligned[i][0] = g+next_g+second_next_g
                        aligned[i][1] = "i:"
                        aligned[i+1] = [eps, eps]
                        aligned[i+2] = [eps, eps]
                        aligned[i] = tuple(aligned[i])
                        continue
                        
                
                if next_p == ":" or next_p =="<eps>:" and not p == eps: 
                    
                    if i < len(aligned)-2:
                        if second_next_g.lower() in vowels: # differentiates separating h as in "gehen" from lengthening h as in "Kahn": 
                                                            #unrefined [('g', 'g'), ('e', 'e'), ('h', ':'), ('e', '@'), ('n', 'n')] - refined [('g', 'g'), ('e', 'e:'), ('h', '<eps>'), ('e', '@'), ('n', 'n')]
                                                            #unrefined [('K', 'k'), ('a', 'a'), ('h', ':'), ('n', 'n')]
                                                            #refined [('K', 'k'), ('ah', 'a:'), ('n', 'n')]
                            aligned[i][1] += ":"
                            aligned[i+1][1] = eps
                            aligned[i] = tuple(aligned[i])
                            continue
                            
                    if next_g == eps:# (e, e), (<eps>, :) to (e, e:), (<eps>, <eps>)
                        aligned[i][1] = p+next_p
                        aligned[i+1][1] = eps
                    else:
                        if next_g.lower() in vowels or next_g.lower() == "h":  # to handle alignments such as [('b', 'b'), ('u', 'u'), ('c', ':'), ('h', 'x'), ('e', '@'), ('n', 'n')]
                            aligned[i][0] = g+next_g
                            aligned[i][1] = p+next_p
                            aligned[i+1]= [eps, eps]
                        else: #ggf. unnecessary here?
                            aligned[i][1] = p+next_p
                            aligned[i+1][1]= eps
                
                
                if p == "i" and next_p == "<eps>" and g == "i" and next_g == "e": # to handle "vielleicht": ('v', 'f'), ('i', 'i'), ('e', '<eps>'), ('ll', 'l'), ('ei', 'aI'), ('ch', 'C'), ('t', 't')
                    aligned[i][0] = g+next_g
                    aligned[i+1] = [eps, eps]
                    
                
                if i < len(aligned)-2:
                    if second_next_p == ":" and next_p == eps:
                        aligned[i][1] = p+second_next_p
                        aligned[i+2][1] = eps
                    
                # [aI] [OY] [aU]
                if p == "a" and next_p == "I" or  p == "O" and next_p == "Y" or p == "a" and next_p == "U": #(x, a), (y, I) to (xy, aI), (<eps>, <eps>) etc. 
                    if not g == eps or next_g == eps: #see wrong BAS output for 'v e r m i ss t e', "f 'E 6. m a.I s. t @ with syllable boundary between a and I
                        aligned[i][0] = g+next_g
                        aligned[i][1] = p+next_p
                        aligned[i+1]= [eps, eps]
            
                # er
                if p == eps and next_p == "6" and g.lower() == "e" and next_g.lower() == "r":  #(e, <eps>), (r, 6) to (<eps>, <eps>), (er, 6)
                    aligned[i][0] = eps
                    aligned[i+1][0] = g+next_g
                
                # double consonants
                if g == next_g and next_p == eps or g == next_g and next_p == "?": # (p, p) (p, <eps>) to (pp,p)(<eps>, <eps>) and (p, p) (p, ?) to (pp,p)(<eps>,?) 
                    aligned[i+1][0] = eps 
                    aligned[i][0] = g+next_g
                    
                if  g == next_g and p == eps: #e.g. <sabbern>
                    aligned[i+1][0] = g+next_g
                    aligned[i] = [eps, eps]
                
                # ck
                if g.lower() == "c" and next_g.lower() =="k" and p == "k": 
                    aligned[i+1] = [eps, eps] 
                    aligned[i][0] = g+next_g
                    aligned[i][1] = p
                    
                elif g.lower() == "c" and next_g.lower() =="k" and next_p == "k": 
                    aligned[i+1] = [eps, eps] 
                    aligned[i][0] = g+next_g
                    aligned[i][1] = next_p
                
                # x
                
                """
                if i < len(aligned)-2:
                    second_next_g = aligned[i+2][0]
                    second_next_p = aligned[i+2][1]  
                          
                    if p == "k" and second_next_p =="s" and next_p == eps:
                        aligned[i][0] = re.sub(eps, "", g+next_g+second_next_g)
                        aligned[i][1] = re.sub(eps, "", p+next_p+second_next_p)
                        aligned[i+1] = [eps, eps]
                        aligned[i+2] = [eps, eps]
                    elif p == eps and  next_p == "k" and second_next_p == "s":
                        aligned[i][0] = re.sub(eps, "", g+next_g+second_next_g)
                        aligned[i][1] = re.sub(eps, "", p+next_p+second_next_p)
                        aligned[i+1] = [eps, eps]
                        aligned[i+2] = [eps, eps]
                """
                        
                if p == "k" and next_p == "s" and g == "x":# (x, k), (<eps>, s) to (x, ks), (<eps>, <eps>)
                    if next_g == eps:
                        aligned[i][1] = p+next_p
                        aligned[i+1][1] = eps
                    else:
                        aligned[i][0] = g+next_g
                        aligned[i][1] = p+next_p
                        aligned[i+1] = [eps, eps]
                     
                
                # qu
                if g.lower() == "q" and next_g.lower() == "u" and p == "k" and next_p == "v": #(q, k), (u, v) to (qu, kv)
                    aligned[i][0] += next_g
                    aligned[i][1] += next_p
                    aligned[i+1] = [eps, eps]
                    
                
                # z
                if next_g.lower() == "z" and g == eps and p == "t" and next_p == "s":# (<eps>, t), (z, s) to (z, ts), (<eps>, <eps>)
                    aligned[i+1][1] = p+next_p
                    aligned[i][1] = eps
                    
                elif g.lower() == "z" and next_g == eps and p == "t" and next_p == "s":# (z, t), (<eps>, s) to (z, ts), (<eps>, <eps>)
                    aligned[i][1] = p+next_p
                    aligned[i+1][1] = eps
                   
                elif g.lower() == "z" and next_g.lower() != "s" and p == "t" and next_p == "s":# ('z', 't'), ('e', 's'), ('r', '6') to ('z', 'ts'), ('er', '6')
                    aligned[i][1] = p+next_p
                    aligned[i+1][1] = eps
                    
                # ng 
                if g == "n" and p == eps and next_g == "g" and next_p =="N": #(n, <eps>), (g, N) to  (<eps>,<eps>), (ng, N)
                    aligned[i][0] = eps
                    aligned[i+1][0] = g+next_g
                    
                # ch 
                if g.lower() == "c" and p == eps and next_g.lower() == "h" and next_p =="C": #(c, <eps>), (h, C) to  (<eps>,<eps>), (ch, C)
                    aligned[i][0] = eps
                    aligned[i+1][0] = g+next_g
                    
                elif g.lower() == "c" and p == eps and next_g.lower() == "h" and next_p =="x": #(c, <eps>), (h, C) to  (<eps>,<eps>), (ch, C)
                    aligned[i][0] = eps
                    aligned[i+1][0] = g+next_g
                    
                elif g.lower() == "c" and next_g.lower() == "h" and p =="k": #(c, k), (h, <eps>) to  (ch, k), (<eps>,<eps>)
                    aligned[i][0] = g+next_g
                    aligned[i+1] = [eps,eps]
                
                # sch 
                if g.lower() == "s" and p == "S" and next_g == "c" and next_p == eps and aligned[i+2][0] == "h" and aligned[i+2][1] == eps: # (s, S) (c, <eps>), (h, <eps>) to  (sch, S), (<eps>,<eps>), (<eps>,<eps>)
                    aligned[i][0] = g+next_g+aligned[i+2][0]
                    aligned[i+1][0] = eps
                    aligned[i+2] = list(aligned[i+2])
                    aligned[i+2][0] = eps
                
                
                #dt
                if g.lower() == "d" and p == eps and next_g == "t" and next_p == "t": #(d, <eps>), (t, t) to (<eps>, <eps>), (dt, t)
                    aligned[i] = [eps, eps]
                    aligned[i+1][0] = g+next_g
                    aligned[i+1][1] = "t" 
                
                #th
                if g.lower() == "t" and p == "t" and next_g == "h" and next_p == eps: #(t, t), (h, <eps>) to (th, t), (<eps>, <eps>)
                    aligned[i][0] = g+next_g
                    aligned[i][1] = "t"
                    aligned[i+1] = [eps, eps]
                    
                #ph
                if g.lower() == "p" and p == "f" and next_g.lower() == "h" and next_p == eps: #(p, f), (h, <eps>) to (ph, f), (<eps>, <eps>)
                    aligned[i][0] = g+next_g
                    aligned[i][1] = "f"
                    aligned[i+1] = [eps,eps]
                    
                #ay in okay
                if g.lower() == "a" and p == "e" and next_g.lower() == "y" and next_p == ":": #('a', 'e:'), ('y', '<eps>')
                    
                    aligned[i][0] = g+next_g
                    aligned[i][1] = p+next_p
                    aligned[i+1] = [eps,eps]
                
                #remove superfluous schwa ('<eps>', '@') e.g. in zerbrochene (tsE6b@rOx@n@) and document in exceptions file
                #if p == "@" and g == eps:
                #    
                #    erroneous = "".join(g for (g,p) in aligned)+"\t" + "".join(p for (g,p) in aligned)
                #    erroneous = re.sub(eps, "", erroneous)
                #    if document_errors: print(erroneous, file = exception_file)
                #    
                #    aligned[i][1] = eps 
                
                #change ('c', '<eps>'), ('<eps>', 'k') to ('c', 'k'), (<eps>, <eps>)
                if p == eps and next_g == eps:
                    aligned[i] = [g, next_p]
                    aligned[i+1] = [eps,eps]    
                    
        # remove <eps> if it appears with another character as in ('ch', '<eps>:')
        if re.search("<eps>", aligned[i][0]) and len(aligned[i][0]) > 5:
            aligned[i][0] = re.sub("<eps>", "", aligned[i][0] )
        

        if re.search("<eps>", aligned[i][1]) and len(aligned[i][1]) > 5:
            aligned[i][1] = re.sub("<eps>", "", aligned[i][1])
        
        # make <eps> out of an empty string if there accidentally happened to be one (e.g. if it was <eps><eps> previsously)
        if len(aligned[i][0]) == 0:
            aligned[i][0] = eps
            
        if len(aligned[i][1]) == 0:
            aligned[i][1] = eps
                     
        aligned[i] = tuple(aligned[i]) #back to tuple
    
    aligned = [tuple for tuple in aligned if tuple != (eps, eps)]   # remove (<eps>, <eps>) tuples  
    #print("refined", aligned)
    #print(aligned, file=alignment_output)
    return aligned


#insert syllable boundaries and stress marks to aligned phonemes
# g2p_aligned = list of tuples of (graphemes, phonemes)
# syllable_dictionary: dictionary of form {word : transcription w/ syllables}
# user_syllable_file: file to which unknown words are added for future use in the form word;transcription
def insert_syllables(g2p_aligned, bas_dictionary = None, document_errors = True):
    
    #exception_file = open("childlex/bas_exceptions_autocorrect.txt", mode="r+", encoding="utf-8")
    
    word = "".join(re.sub("<eps>", "",graphemes) for (graphemes, phonemes) in g2p_aligned)
    #print(g2p_aligned)
    
    """if syllable_file != None:
        found = False
        syllable_file = open(syllable_file, mode = "r", encoding = "utf-8").read().splitlines()    
        
        if user_syllable_file != None:
            user_syllable_file = open(user_syllable_file, mode ="r+", encoding ="utf-8")
            user_syllable = user_syllable_file.read().splitlines()
            syllable_file = syllable_file + user_syllable
        
        
        syllable_dictionary = {re.sub(" ", "", line.split(";")[0]):re.sub(" ", "", line.split(";")[1]) for line in syllable_file if len(line.split(";")) == 2}"""
    
    if bas_dictionary != None:    
        #if word in bas_dictionary: #all words should be there (added when phonemes were looked up)!
        bas_output = bas_dictionary[word].split(";")
        syllables = re.sub(" ", "", bas_output[1])
            
        #edit 25.5.17: ignore titlecase (usually sentence initial) when looking up word 
        #we assume that titlecase/lowercase variant have the same properties, except for <weg>/<Weg> but they are both included in the file 
        #edit 8.8.17: should already have been added to bas_dict when looking up phonemes!
        #elif word.istitle() and word.lower() in bas_dictionary:
        #    bas_result = bas_dictionary[word.lower()]
        #    bas_result = bas_result[0].title() + bas_result[1:]
        #    syllables = bas_result.split(";")[1]
            
        #else: unknown words should all have been added to the BAS file
            #if word unknown, get from BAS web service
            #syllables = getSingleBAS_g2p_syllables(word)
            
            #add to user syllable file
            #if user_syllable_file != None: 
            #    user_syllable_file = open(user_syllable_file, mode ="a", encoding ="utf-8")
            #    print(syllables.strip(), file= user_syllable_file)
            #    user_syllable_file.close()
                    
            #syllables = syllables.split(";")
            #if len(syllables) == 2:
                
                #extract syllables               
            #    target_word = re.sub(" ", "", syllables[0])
            #    syllables = re.sub(" ", "", syllables[1])
            #    syllables = re.sub(r"\n", "", syllables)
                
                #add to dictionary 
                #syllable_dictionary[target_word] = syllables

        
        
        
        """
        #alter Weg über Liste, wahrscheinlich löschen
        syllable_file = [line.split(";") for line in syllable_file]
        if index != None:
            syllables = re.sub(" ", "", syllable_file[index][1])
        else:
            for i in range(len(syllable_file)):
                line = syllable_file[i]
                if re.sub(" ", "", line[0]) == word:
                    syllables = re.sub(" ", "", line[1])
                    found = True
                    #print(word)
            if found == False:
                #print(word)
                syllables = getSingleBAS_g2p_syllables(word)
                if user_syllable_file != None: print(syllables, file= user_syllable_file)
                syllables = syllables.split(";")
                syllables = re.sub(" ", "", syllables[1])
                syllables = re.sub(r"\n", "", syllables)
        """       

    
    else:
        syllables = getSingleBAS_g2p_syllables(word)
        syllables = syllables.split(";")
        syllables = re.sub(" ", "", syllables[1])
        syllables = re.sub(r"\n", "", syllables)
      
    #print(syllables)
    
    #edit 30.11.: these are no longer repaired within the program but externally
    #repair wrong BAS output that causes errors in the further processing (when converting to EXMARaLDA)
    #print(word, syllables)
    #if word == "Fr." and syllables == "?Ef.?'Er": 
    #    syllables =  "'fr"
    #    if document_errors: print(word, "\t", syllables, file=exception_file)
    #    
    #if word == "antworteten" and syllables == "?ant.v'O6.t@.?Et.@n": 
    #    syllables =  "?'ant.vO6.t@.t@n"
    #    if document_errors: print(word, "\t", syllables, file=exception_file)
    #    
    #if word == "verbindete" and syllables == "fE6.b'@.?In.d@.t@": 
    #    syllables =  "fE6.b'In.d@.t@"
    #    if document_errors: print(word, "\t", syllables, file=exception_file)
    
    
    
    ##correct other systematic syllable errors
    
    #if a syllable boundary occurs between a consonant and a vowel, shift it before the consonant (e.g. l'ax.@n -> l'a.x@n)
    syllables = re.sub(r"([^aeiouy92EIOUY@6])(\.)('?)([aeiouy92EIOUY@6])", r"\2\1\3\4", syllables) 
    
    #print(syllables)
    #print(g2p_aligned)
    
    ##### insert syllables into the phonemes that are aligned with graphemes
    
    syll_counter = 0 #for iterating through the phonemes with syllables to compare them with the aligned phonemes without syllable marks to see where they have to be inserted
    
    for i in range(len(g2p_aligned)):
        
        syll_inserted = [] # stores the phonemes of the current alignment unit with syllable marks
                
        if g2p_aligned[i][1] == "<eps>":continue #if no phoneme present (e.g. <h> in <gehen>)
        
        g2p_aligned[i] = list(g2p_aligned[i])
        g2p_aligned[i][1] = [p for p in g2p_aligned[i][1]] # each letter of the current phoneme  as list element (e.g. 'ts' -> ['t','s'])
        
        j = 0
        while j < len(g2p_aligned[i][1]):
            
            phoneme = g2p_aligned[i][1][j] 
            if syllables[syll_counter] == phoneme: #if the current element is a phoneme, it is stored in the final list
                syll_inserted.append(phoneme)
                syll_counter += 1
                j += 1
                
            elif  syllables[syll_counter] == "." or syllables[syll_counter] =="'" :
                syll_inserted.append(syllables[syll_counter]) #if the current element is not a phoneme but a syllable mark, store this in the final list
                syll_counter += 1
            
            else:
                syll_counter += 1 #all other characters are ignored (e.g. a superfluous schwa as in <zerbrochene> (tsE6b@rOx@n@) that was removed in refine_g2p_alignment) -> NOT ANYMORE!
                
        if syll_counter < len(syllables): #I want syllable boundaries to appear after the last phoneme of a syllable not before the first of the next syllable
            if syllables[syll_counter] == ".":
                syll_inserted.append(".")
                syll_counter += 1
        
        syll_inserted = "".join(syll_inserted) 
        cleaned_syll_inserted =  re.sub(r"(?<=^)\.", "", syll_inserted) #remove syllable boundaries at the beginning of an alignment unit or preceded by a stress mark
        cleaned_syll_inserted =  re.sub(r"(?<=^')\.", "", syll_inserted)#(have to be residues of deleted characters as in <zerbrochene> (tsE6b@rOx@n@) #edit: no characters are deleted anymore, (tsE6b@rOx@n@) is left as it is! So probably these lines are superfluous now...
            
                
    #        if with_syllables >= len(syllables)-2: break
            
    #        try:
    #            print(with_syllables)
    #            if syllables[with_syllables] == "'":
    #                print("hier")
    #                g2p_aligned[i][1][j] = "'"+ g2p_aligned[i][1][j]
                    
                    #break#...
                
    #                if syllables[with_syllables+2] == ".":
    #                    g2p_aligned[i][1][j] += "."
    #                    with_syllables += 2
                        #break #...
    #                else:
    #                    with_syllables += 1
                        
    
    #            if syllables[with_syllables+1] == ".":
    #                g2p_aligned[i][1][j] += "."
    #                with_syllables += 2
                    #break#...
    
    #            else:
    #                with_syllables += 1
    #        except:
    #            with_syllables += 1
    #            print(word, " syllable error")
        
        
        g2p_aligned[i][1] = cleaned_syll_inserted
        g2p_aligned[i] = tuple(g2p_aligned[i])
     
    #print(g2p_aligned)           
    return g2p_aligned
            

#make a dictionary to count correspondences of graphemes and phonemes
def correspondences(aligned):
    dict = {}
    for tuple in aligned:
        if tuple not in dict:
            dict[tuple] = 1
        else:
            dict[tuple] += 1
    return dict

#wordlist is either a text file containing only orthographic words or a csv-file with orthographic words in the left column and its phonetics in the right column separated by a tab
#compute correspondences of graphemes and phonemes and count them
def run_counting(wordlist):
    all_alignments = []
    
    list = open(wordlist, mode="r", encoding="utf-8").read().splitlines()
    try:
        if len(list[0].split()) == 1: # if only orthographic words are provided, get pronunciation from BAS
            bas = getBAS_g2p(wordlist).strip()
            graph_phon = [(line.split(";")[0], re.sub(" ", "", line.split(";")[1])) for line in bas.splitlines()] # List of (word, pronunciation) tuples
        elif len(list[0].split()) == 2: # if pronunciation is given, use this
            graph_phon = [(line.split()[0],line.split()[1]) for line in list] # List of (word, pronunciation) tuples
    except:
        print("Something is wrong with your input format. Please make sure that you either provide one (orthographic) word per line or an orthographic word and its pronunciation separated by a tab!")
        return
    for (graphemes, phonemes) in graph_phon:
        aligned = g2p_align(graphemes, phonemes)
        aligned = refine_g2p_align(aligned)
        all_alignments.extend(aligned)
    corres = correspondences(all_alignments)
    for key in sorted(corres, key=corres.get, reverse= True):
        print(key, "\t", corres[key])

#run_counting("test.txt") 
#run_counting("frequent_words_ignorecase.txt")   


  
        
        
        
        
    