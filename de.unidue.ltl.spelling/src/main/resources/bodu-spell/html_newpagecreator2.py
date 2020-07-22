#coding=utf-8

'''
Created on 24.11.2016

@author: laarmann-quante
'''

# from yattag import Doc
# from yattag import indent

# import errors_baseRate
# import xmltoken
import OLFA
import re
# from _operator import itemgetter
# import requests
import xml.etree.ElementTree as ET
# import wave
# import sys, os
import argparse
# import LearnerXMLToEXMARaLDA_new
import jsonpickle

def create_all_html_pages(infile, tagset="Litkey", outputlang="en"): #tagset can be "Litkey" or "OLFA", outputlang="en" or "de"
    
    #use the name of the infile to create a folder in which the html pages of the analysis are stored, inside the superordinate folder
    #e.g. "test/test.txt" -> "test/results_test/" 
    plain_name = re.sub("(\.csv|\.txt)", "", infile)
    
    m = re.search("(\w+\/)(\w+(\.txt|\.csv))", infile)
    if m:
        superfolder = m.group(1)
    else:
        superfolder = ""
    
    textname = re.sub(superfolder, "", plain_name)


    '''
    outfolder = superfolder + "results_" + textname + "/"
    if not os.path.exists(outfolder+"errorpages/"):
        os.mkdir(outfolder)
        os.mkdir(outfolder+"errorpages/")
    
    doc, tag, text, line = Doc().ttl()
    '''
    
    #############################
    # new class with OLFA errors used! (also has everything else from errors_baseRate.py
    old = "childlex/litkey_corrected.g2p.tab"
    tokens = OLFA.get_errors_with_olfa(infile, textname=plain_name, bas_file="childlex/hunspell_DE_bas.txt", user_bas_file="childlex/user_bas_file.g2p.tab")
    # Marie: simplify output since we only need errors and target string
    for i in range(0,len(tokens)):
        json_obj = jsonpickle.encode(tokens[i].geterrors(),make_refs=False)
    #print("Tokens",tokens.getTarget(),tokens.geterrors())
    #json_obj = jsonpickle.encode(tokens, make_refs=False)
        print(tokens[i].get_target(),"\t",json_obj)



    '''
    # create json-dump of get_errors_with_olfa output (will serve as input for boduspell-app)
    if not os.path.exists('outputs'):
        os.mkdir('outputs')
    with open('outputs/output_' + textname + '.json', 'w') as outfile:
        json_obj = jsonpickle.encode(tokens)
        outfile.write(json_obj)
    # return json_obj
    '''
    '''
    if tagset=="OLFA": 
        for token in tokens:
            
            for e in token.geterrors() + token.getPossibleErrors():
                e.category = e.olfa
                
     
    #mystrings = [("Dies", True), ("sind", False), ("verlinkte", False), ("Wörter", True)]
    
    #orig_dict is a dictionary of the form
    # {orig_string1 : count, orig_string2 : count, ...}
    #when counting committed and possible errors, the error count has to be multiplied by the times the orig string occurred
    
    orig_dict =  {}
    
    # errors {} is a 
    #dictionary of the form { (orig_string, target_string) : 
    #                                   { 'possible' :
    #                                                    { error_cat : [([orig_range], [target_range]), ([orig_range], [target_range]) } #if error possible more than once in this word
    #                                                    { error_cat : [([orig_range], [target_range])] }
    #
    #                                     'committed' :    
    #                                                    { error_cat : [([orig_range],  [target_range])] }
    #                                                    { error_cat : [([orig_range],  [target_range])] }
    #                                    }
    #                        }
    # (each orig type only occurs once)
    
    errors = {}

    
    #filter which errors can sensibly be displayed under possible errors -> #ignore possible errors where another error occurred 
    #(e.g. <Wise> is Vlong_i_ie but as ie was not spelled correctly, it would not make sense to say that Vlong_ieh_ie was applied correctly
    def possible_errors_to_consider(token):
        possible_errors_to_consider = []
        
        pes = token.getPossibleErrors()
        errors_to_consider = token.geterrors()
  
        for pe in pes:   
            found = False
            for err in errors_to_consider:
                if pe.range == err.range: #ignore possible errors where another error occurred
                    if pe.category == err.category:
                        found = False
                    else:
                        found = True
            if found == False:
                possible_errors_to_consider.append(pe)
        
        return possible_errors_to_consider
    
    
    for token in tokens:
        if (token.orig, token.target) not in orig_dict: #there can be different targets for the same orig, e.g. <der> for "der/Der"
            orig_dict[(token.orig, token.target)] = 1
        else:
            orig_dict[(token.orig, token.target)] += 1
        
        errors[(token.orig, token.target)] = {"possible" : {}, "committed" : {} }
        
        #print(token.getAlignedTargetPhonemeIndices())
        #print(token.characters_aligned)
        
    
        
        #get orig range of an error
        for e in possible_errors_to_consider(token):
            
            found = False
            orig_range = []
            for alignment in token.characters_aligned:
                if alignment[1]!= ['<eps>', '<eps>']:
                    if e.range[0] in range(alignment[1][0], alignment[1][1]+1):
                        orig_range.append(alignment[0][0])
                        found1 = True
                    if found1:
                        if e.range[1] in range(alignment[1][0], alignment[1][1]+1):
                            orig_range.append(alignment[0][1])
                            found = True
            if found:        
                if e.category not in errors[(token.orig, token.target)]["possible"]:
                    errors[(token.orig, token.target)]["possible"][e.category] = [(orig_range, e.range)]
                elif e.category in errors[(token.orig, token.target)]["possible"] and (orig_range, e.range) not in errors[(token.orig, token.target)]["possible"][e.category]: #no doubled entries because the possible error produces multiple error candidates 
                    errors[(token.orig, token.target)]["possible"][e.category].append((orig_range,e.range))       
                
    
        for e in token.geterrors():
            found = False
            orig_range = []
            for alignment in token.characters_aligned:
                if alignment[1]!= ['<eps>', '<eps>']:
                    if e.range[0] in range(alignment[1][0], alignment[1][1]+1):
                        orig_range.append(alignment[0][0])
                        found1 = True
                    if found1:
                        if e.range[1] in range(alignment[1][0], alignment[1][1]+1):
                            orig_range.append(alignment[0][1])
                            found = True
            if found:        
                if e.category not in errors[(token.orig, token.target)]["committed"]:
                    errors[(token.orig, token.target)]["committed"][e.category] = [(orig_range, e.range)]
                elif e.category in errors[(token.orig, token.target)]["committed"] and (orig_range, e.range) not in errors[(token.orig, token.target)]["committed"][e.category]: #no doubled entries because the possible error produces multiple error candidates 
                    errors[(token.orig, token.target)]["committed"][e.category].append((orig_range,e.range))       
    
    #print(errors)
    
    # error_counts {} is a dictionary of the form
    # { error_category : [number committed, number possible] , ...}, e.g.
    error_counts = {}
    
    for tuple in errors:
        for category in errors[tuple]["possible"]:
            if category not in error_counts:
                error_counts[category] = [0,len(errors[tuple]["possible"][category])*orig_dict[tuple]]
            else:
                error_counts[category][1] += len(errors[tuple]["possible"][category])*orig_dict[tuple]
    for tuple in errors:
        for category in errors[tuple]["committed"]:
            if category not in error_counts:
                error_counts[category] = [len(errors[tuple]["committed"][category])*orig_dict[tuple],"-"] #categories such as del can only be "committed" but not forseen under "possible"
            else:    
                error_counts[category][0] += len(errors[tuple]["committed"][category])*orig_dict[tuple]
    
    #print(error_counts)
    
    
    def getMaryTTS(tokenstring):
        
        url = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/services/runTTS"
            
        r = requests.post(url+"?AUDIO=WAVE_FILE&INPUT_TYPE=TEXT&INPUT_TEXT="+tokenstring+"&VOICE=bits3unitselautolabelhmm&OUTPUT_TYPE=AUDIO")
        
        #print()
        #print(r.status_code)
        #print(r.text)
        
        #download result file
        root = ET.fromstring(r.text)
        downloadLink = root.find("downloadLink").text
        
        result = requests.get(downloadLink)
            
        with open(outfolder+"errorpages/"+tokenstring+".wav", mode="wb") as wav:
            wav.write(result.content)
    
    ################################################################################################################################################
    
    language = outputlang
    def t(text):
        ger_eng = {
                   "Alle Fehler" : "All errors",
                   "Anzahl Wörter:" : "Number of words:",
                   "Anzahl Fehler:" : "Number of errors:",
                   "Anzahl fehlerhafter Wörter:" : "Number of misspelled words:", 
                   "Klicke auf einen Fehler, um nähere Informationen angezeigt zu bekommen" : "Click on an error to show more information",
                   "Oder wähle eine bestimmte Fehlerkategorie aus:" : "Or choose a particular error category",
                   "Fehlerkategorie" : "Error category",
                   "falsch" : "wrong", 
                   "falsch:" : "wrong:",
                   "alle" : "all",
                   "Dies ist ein Fehler der Kategorie": "This is an error of category ", 
                   "richtig wäre" : "correct",
                   "Alle Fehler anzeigen" : "Show all errors",
                   "Fehler und korrekte Verschriftungen der Kategorie" : "Errors and correct spellings of category ",
                   "Fehlerrate:": "Error rate: ",
                   "Hier wurde": "here ",         
                   "richtig angewandt":" was used correctly" ,
                   "Möglicher Fehler:":"Possible error:",
                   "richtig wäre:": "correct would be:",
                   "Die korrekte Schreibung kann von einer verwandten Form hergeleitet werden": "The correct spelling can be inferred from a related word form",
                   "Die Schreibung des Lerners ist phonetisch plausibel" : "The spelling of the learner is phonetically plausible",
                   "Die Schreibung des Lerners ist phonetisch nicht plausibel" : "The spelling of the learner is not phonetically plausible",
                   "Die Schreibung des Lerners ist umgangssprachlich plausibel" : "The spelling of the learner is colloquially plausible"
                   }
        
        if language == "de":
            return text
        
        elif language == "en" and text.strip() in ger_eng:
            return ger_eng[text.strip()]
        
        elif KeyError:
            print(text, "nicht vorhanden!")
            sys.exit()
        
        else:
            print("Fehler bei der Sprache!")
            sys.exit()
    
    doc.asis('<!DOCTYPE html>')
    with tag('html'):
        with tag ("head"):
            doc.stag("meta", charset="utf-8")
            doc.stag("link",('type', 'text/css'), rel="stylesheet", href="../../test.css") #tupel müssen vor keywords kommen
        with tag('body'):
            
            #for word in mystrings:
            #    with tag("a", href="test.html"):
            #        if word[1] == True:
            #            doc.attr(klass="correct")
            #        elif word[1] == False:
            #            doc.attr(klass="incorrect")
            #        text(word[0])
            #    text(" ")
            
            with tag("h3"):
                text(t("Alle Fehler"))
            
            with tag('div', id="textfield"):
                for token in tokens:
                    #print(token.orig)
                    with tag('span', klass="orig-token"):
                        if token.geterrors() != []:
                            #with tag('span', klass="incorrectword"):
                                #print(token.characters_aligned)
                                
                                
                                for a in range(len(token.characters_aligned)):
                                    alignment = token.characters_aligned[a]
                                    found = False
                                    if alignment[1]!= ['<eps>', '<eps>']: #target not empty
                                        for i in range(alignment[1][0], alignment[1][1]+1):
                                            if found:break
                                            for e in token.geterrors():
                                                if e.category != "hyp_separating_h" and not e.category.startswith("del_"): #otherwise in schreihen, the <i> and <e> before and after the <h> would be marked too // in the del cases the error range is not the target range but the orig range!
                                                    #print(e.range)
                                                    if found:break
                                                    for j in range(e.range[0], e.range[1]+1):
                                                        if found:break
                                                        if j == i: 
                                                            
                                                            fix_3_2 = False
                                                            #look if the next target character is empty and not part of a del error -> then it is a 3:2 alignment like iehr -> ihr and the 3rd orig character has to be added to the error range as well           
                                                            if a < len(token.characters_aligned)-1:
                                                                next_alignment = token.characters_aligned[a+1]
                                                                if next_alignment[1] == ['<eps>', '<eps>'] and all(err.range != next_alignment[0] for err in token.geterrors() if err.category.startswith("del_")):
                                                                    fix_3_2 = True 
                                                            
                                                            #adjust error spans visually (adjust the margin of the red marking)
                                                            if j == e.range[0] and j == e.range[1]:
                                                                css_class = "incorrect"
                                                            elif j == e.range[0]:
                                                                css_class = "incorrectleft"
                                                            elif j == e.range[1]:
                                                                css_class = "incorrectright"
                                                                if fix_3_2 == True:
                                                                    css_class = "incorrectmiddle" 
                                                            else:
                                                                css_class = "incorrectmiddle"
                                                             
                                                                            
                                                            with tag('a', klass=css_class):
                                                                if alignment[0] != ['<eps>', '<eps>']:
                                                                    text(token.orig[alignment[0][0]:alignment[0][1]+1])
                                                                    doc.attr(href="errorpages/"+token.orig+"_"+e.category+".html")
                                                                else:
                                                                    doc.asis("&nbsp;")
                                                                    doc.attr(href="errorpages/"+token.orig+"_"+e.category+".html")
                                                                                                                
                                                                with tag('span', klass="tooltiptext"):
                                                                    text(e.category)
                                                                    
                                                            
                                                            found = True
                                                            break
                                                        
                                                        
                                        if found == False:
                                            text(token.orig[alignment[0][0]:alignment[0][1]+1])  
                                
                                    else: #target is empty [<eps>, <eps>]
                                        if all(err.range != alignment[0] for err in token.geterrors() if err.category.startswith("del_")):
                                            css_class = "incorrectright"
                                        else:
                                            css_class = "incorrect"
                                        with tag('a', klass=css_class):
                                            
                                                text(token.orig[alignment[0][0]:alignment[0][1]+1])
                                                doc.attr(href="errorpages/"+token.orig+"_"+e.category+".html")
                                                                                                            
                                                with tag('span', klass="tooltiptext"):
                                                    text(e.category)
                                
                        else:
                            text(token.orig)
                        
                        with tag("span", klass="target"):
                            text(token.target)
    
                
            with tag('div', id='infobox'):
                text(t("Oder wähle eine bestimmte Fehlerkategorie aus:"))
                doc.stag("br")
                doc.stag("br")
                with tag('table'):
                    with tag("tr"):
                        line("th", t("Fehlerkategorie"))
                        line("th", t("falsch"))
                        line("th", t("alle"))
                    error_counts_without_baserate = {}
                    error_counts_with_baserate = {}
                    #make separate dictionaries for errors with and without baserate
                    for error in error_counts:
                        if error_counts[error][1] == "-":
                            error_counts_without_baserate[error] = error_counts[error]
                        else:
                            error_counts_with_baserate[error] = error_counts[error]
                            
                    for category, count in sorted(error_counts_with_baserate.items(), key=lambda x: (x[1][0], x[1][0]/x[1][1], x[1][1], x[0]), reverse=True): #sorted firstly by "false", secondly by the ratio of "false" and "possible", thirdly "possible" and fourthly by "error category" in reverse order
                        #print(category, count)
                        if count[0] > 0:   
                            with tag("tr"):
                                with tag("td"):
                                    with tag("a", href="errorpages/"+category+".html"):
                                        text(category)
                                with tag("td"):
                                    text(count[0])
                                with tag("td"):
                                    text(count[1]) 
                                if count[0] / count [1] < .5:
                                    doc.attr(klass = "yellow-row")
                                else:
                                    doc.attr(klass = "red-row")
                        else:
                            if error_counts_without_baserate != {}:
                                for cat2, count2 in sorted(error_counts_without_baserate.items(), key=lambda x: (x[1][0], x[0]), reverse=True):
                                    with tag("tr"):   
                                        with tag("td"):
                                            with tag("a", href="errorpages/"+cat2+".html"):
                                                text(cat2)
                                        with tag("td"):
                                            text(count2[0])
                                        with tag("td"):
                                            text(count2[1]) 
                                        doc.attr(klass = "red-row") 
                                    error_counts_without_baserate = {}
                            else: #if errors without baserate were already processed
                                with tag("tr"):
                                    with tag("td"):
                                        with tag("a", href="errorpages/"+category+".html"):
                                            text(category)
                                    with tag("td"):
                                        text(count[0])
                                    with tag("td"):
                                        text(count[1]) 
                                    
                                    doc.attr(klass = "green-row")
                                    
                               
            with tag('div', id="mainpageinfo"):#, style="clear:both"):
                with tag('table'):
                    with tag("tr"):
                        with tag("td", style="width:300px"):
                            text(t("Anzahl Wörter: "))
                            word_counter = len([tok for tok in tokens if any(c.isalpha() for c in tok.target)])
                            text(str(word_counter))
                        with tag("td", style="width:400px"):
                            text(t("Anzahl fehlerhafter Wörter: "))
                            misspelled_word_counter = len([tok for tok in tokens if tok.target != tok.orig and tok.target != ""])
                            text(str(misspelled_word_counter))
                        with tag("td",  style="width:300px"):
                            text(t("Fehlerrate: "))
                            text(str(round(misspelled_word_counter/word_counter*100,2))+"%")
                        with tag("td", style="width:300px"):
                            text(t("Anzahl Fehler: "))
                            err_counter = 0
                            for cat in error_counts:
                                err_counter += error_counts[cat][0]
                            text(str(err_counter))
                doc.stag("br")
                text(t("Klicke auf einen Fehler, um nähere Informationen angezeigt zu bekommen"))                   
            
    o = open(outfolder+"index.html", mode="w", encoding="utf-8")
    
    print(doc.getvalue(), file=o)
    #print(indent(doc.getvalue()))
    
    ######################################################################################################################
    
    
    
    
    def create_head():
        doc, tag, text = Doc().tagtext()
    
        with tag ("head"):
            doc.stag("meta", charset="utf-8")
            doc.stag("link",('type', 'text/css'), rel="stylesheet", href="../../../test.css") #tupel müssen vor keywords kommen
                
        return doc.getvalue()
    
    def link_to_main_page():
        doc, tag, text = Doc().tagtext()
        with tag('a', href="../index.html"):
            text(t("Alle Fehler anzeigen"))
            doc.stag("br")
        return doc.getvalue()
    
    
    #marks
    def marked_errors_correct(token, relevant_error_cat):
    
        doc, tag, text = Doc().tagtext()
        fixed_3_2 = False
        
        if token.characters_aligned == []: #e.g. meta-token \h printed directly
            text(token.orig)
            return doc.getvalue()
            
        for a in range(len(token.characters_aligned)):
            alignment = token.characters_aligned[a]
            found = False
            if alignment[1]!= ['<eps>', '<eps>']: #target empty
                for i in range(alignment[1][0], alignment[1][1]+1):
                    if found:break
                    if token.geterrors() != []:   
                        #mark as incorrect where the relevant error category was committed
                        for e in token.geterrors():
                            if e.category == relevant_error_cat: 
                                if e.category != "hyp_separating_h" and not e.category.startswith("del_"): #otherwise in schreihen, the <i> and <e> before and after the <h> would be marked too // in the del cases the error range is not the target range but the orig range so it has to be ignored here!
                                #print(relevant_error_cat, alignment, i, e.category, found)
                                    if found:break
                                    for j in range(e.range[0], e.range[1]+1):
                                        if found:break
                                        
                                        if j == i: #To Do: 2:2 relationen etc. auflösen, z.B. ieh = 1 Spanne  für <i> und nicht drei separate!  
                                            
                                            fix_3_2 = False
                                            #look if the next target character is empty and not part of a del error -> then it is a 3:2 alignment like iehr -> ihr and the 3rd orig character has to be added to the error range as well           
                                            if a < len(token.characters_aligned)-1:
                                                next_alignment = token.characters_aligned[a+1]
                                                if next_alignment[1] == ['<eps>', '<eps>'] and all(err.range != next_alignment[0] for err in token.geterrors() if err.category.startswith("del_")):
                                                    fix_3_2 = True
                                                    
                                            
                                            #adjust error spans visually (adjust the margin of the red marking)
                                            if j == e.range[0] and j == e.range[1]:
                                                css_class = "incorrect"
                                            elif j == e.range[0]:
                                                css_class = "incorrectleft"
                                            elif j == e.range[1]:
                                                css_class = "incorrectright"
                                                if fix_3_2 == True:
                                                    css_class = "incorrectmiddle" 
                                            else:
                                                css_class = "incorrectmiddle"
                                                
                                                
                                            with tag('a', klass=css_class):
                                                if alignment[0] != ['<eps>', '<eps>']:
                                                    text(token.orig[alignment[0][0]:alignment[0][1]+1])
                                                    doc.attr(href=token.orig+"_"+e.category+".html")
                                                else:
                                                    doc.asis("&nbsp;")
                                                    doc.attr(href=token.orig+"_"+e.category+".html")
                                                                                                
                                                with tag('span', klass="tooltiptext"):
                                                    text(e.category)
                                            
                                             
                                            if fix_3_2 == True:
                                                with tag('a', klass="incorrectright"):
                                                        text(token.orig[next_alignment[0][0]:next_alignment[0][1]+1])
                                                        doc.attr(href=token.orig+"_"+e.category+".html")
                                                                                                                                                       
                                                        with tag('span', klass="tooltiptext"):
                                                            text(e.category)
                        
                                                    
                                                fixed_3_2 = True
                                                            
                                                    
                                            found = True
                                            break 
                                        
                    #mark as correct where the relevant error category was possible but not committed
                    for e in possible_errors_to_consider(token):
                        if e.category == relevant_error_cat:    
                            if e.category != "hyp_separating_h" and not e.category.startswith("del_"): #otherwise in schreihen, the <i> and <e> before and after the <h> would be marked too // in the del cases the error range is not the target range but the orig range so it has to be ignored here!
                            #print(relevant_error_cat, alignment, i, e.category, found)
                            
                                if found:break
                                for j in range(e.range[0], e.range[1]+1):
                                    if found:break
                                    if j == i: #To Do: 2:2 relationen etc. auflösen, z.B. ieh = 1 Spanne  für <i> und nicht drei separate!  
                    
                                        
                                        with tag('a', klass="correct"):
                                            if alignment[0] != ['<eps>', '<eps>']:
                                                text(token.orig[alignment[0][0]:alignment[0][1]+1])
                                                doc.attr(href="pe_"+token.orig+"_"+e.category+".html")
                                            else:
                                                doc.asis("&nbsp;")
                                                doc.attr(href="pe_"+token.orig+"_"+e.category+".html")
                                                                                            
                                            with tag('span', klass="tooltiptext"):
                                                text(t("Möglicher Fehler:"))
                                                doc.stag("br")
                                                text(e.candidate)
                                                
                                                
                                        
                                        found = True
                                        break
                    
                            elif e.category == "hyp_separating_h" and not e.range in [er.range for er in token.geterrors() if e.category == "hyp_separating_h"]:
                                if e.range[0] == alignment[1][0] or e.range[1] == alignment[1][1]:
                                   
                                    with tag('a', klass="correct"):
                                        text(token.orig[alignment[0][0]:alignment[0][1]+1])     
                                        doc.attr(href="pe_"+token.orig+"_"+e.category+".html")
                                                                                                            
                                        with tag('span', klass="tooltiptext"):
                                            text(t("Möglicher Fehler:"))
                                            doc.stag("br")
                                            text(e.candidate)
           
                                        found = True
                                        break
                                
                               
            #mark superfluous characters in orig
            else: #target is empty [<eps>, <eps>]
                if fixed_3_2 == True: #skip if the empty target was already marked as an error for a 3:2 alignment like <iehr> -> <ihr>
                    fixed_3_2 = False
                    continue
                for e in token.geterrors():
                    if e.category == relevant_error_cat:
                        if e.category != "hyp_separating_h":
                            if e.range == alignment[0]: #in del cases e.range refers to the orig range ##TO DO: ieh -> ie fixen! wird hier nicht erfasst!
                                with tag('a', klass="incorrect"):
                                        text(token.orig[alignment[0][0]:alignment[0][1]+1])
                                        doc.attr(href=token.orig+"_"+e.category+".html")
                                                                                                    
                                        with tag('span', klass="tooltiptext"):
                                            text(e.category)
    
                                        found = True
                                        break
                        
                        else: # with hyp_separating_h the error range refers to the two surrounding target characters
                            if e.range[0] == token.characters_aligned[a-1][1][1]:
                                with tag('a', klass="incorrect"):
                                    text(token.orig[alignment[0][0]:alignment[0][1]+1])     
                                    doc.attr(href=token.orig+"_"+e.category+".html")
                                                                                                    
                                    with tag('span', klass="tooltiptext"):
                                        text(e.category)
    
                                    found = True
                                    break
                
            
    
                                
                                
            #print the characters where the relevant error category was neither possible nor committed                                
            if found == False:
                    if alignment[0] != ['<eps>', '<eps>']: 
                        text(token.orig[alignment[0][0]:alignment[0][1]+1])  
                    else: #orig empty
                        with tag('span', klass="missing"):
                            doc.asis("&nbsp;")
                            
    
        return doc.getvalue()
        
     
    #error page which shows all errors and correct applications of a particular category 
    #appears when clicking on an error category in the table
    def general_error_page(relevant_error_cat, outfilename):
        
        doc, tag, text = Doc().tagtext()
        doc.asis('<!DOCTYPE html>')
        with tag('html'):
        
            doc.asis(create_head())
               
            with tag('body'):
                
                with tag("h3"):
                    text(t("Fehler und korrekte Verschriftungen der Kategorie "), relevant_error_cat)
                
                with tag('p', id="backtomainpage"):
                    doc.asis(link_to_main_page())
                
                with tag('div', id="textfield"):
                    for token in tokens:
                        
                        with tag('span', klass="orig-token"):
                            
                            doc.asis(marked_errors_correct(token, relevant_error_cat))
                        
                            with tag("span", klass="target"):
                                text(token.target)
                    
                #with tag('p', id="infobox"):
                #    text("Hier wurde ")
                #    with tag("b"):
                #        text(relevant_error_cat)
                #    text(" richtig angewandt")
                
                
    
                        
        
        outfile = open(outfilename, mode="w", encoding="utf-8")
        print(doc.getvalue(), file=outfile)
        #print(indent(doc.getvalue()))
    
     
    #error page which shows all errors and correct applications of a particular category 
    #in addition it highlights the current token (more precisely: all tokens with this orig spelling) + provides an info box about the spelling
    #appears when clicking on a specific error 
    def orig_error_page(relevant_token, relevant_error_cat, outfilename):
        doc, tag, text = Doc().tagtext()
        doc.asis('<!DOCTYPE html>')
        with tag('html'):
        
            doc.asis(create_head())
               
            with tag('body'):
                
                with tag("h3"):
                    text(t("Fehler und korrekte Verschriftungen der Kategorie "), relevant_error_cat)
                 
                with tag('p', id="backtomainpage"):
                    doc.asis(link_to_main_page())   
                
                with tag('div', id="textfield"):
                    
                    for token in tokens:
                        with tag('span', klass="orig-token"):
                            if token.orig == relevant_token.orig:
                                with tag("span", klass="highlight-token"):
                                    doc.asis(marked_errors_correct(token, relevant_error_cat))
                                
                            else:
                                doc.asis(marked_errors_correct(token, relevant_error_cat))
                            
                            with tag("span", klass="target"):
                                text(token.target)
                                
                
                with tag('div', id="infobox"):
                    
                    text(t("Dies ist ein Fehler der Kategorie "))
                    with tag("a", style="text-decoration:None;font-weight:bold"):
                        text(relevant_error_cat)
                        doc.attr(href=relevant_error_cat+".html")
                    doc.stag("br")
                    doc.stag("br")
                    
                    with tag("span", style="text-align:left;float:left;width:50%"):
                        text(t("falsch:"))
                        doc.stag("br")
                        
                        with tag('span', klass="big"): 
                            doc.asis(marked_errors_correct(relevant_token, relevant_error_cat))
                        doc.stag("br")
                        doc.stag("br")
                        
                        #getMaryTTS(relevant_token.orig)
                        #with tag("audio", controls="true"):
                        #    doc.stag("source", src=relevant_token.orig+".wav", type="audio/wav")
                        #    text("Ihr Browser unterstützt keine Audiowiedergabe.")
                    
                        for e in relevant_token.geterrors():
                            if e.category == relevant_error_cat:
                                if e.phon_ok == "true":
                                    doc.stag("br")
                                    text(t("Die Schreibung des Lerners ist phonetisch plausibel"))
                                    doc.stag("br")
                                elif e.phon_ok =="false":
                                    doc.stag("br")
                                    text(t("Die Schreibung des Lerners ist phonetisch nicht plausibel"))
                                    doc.stag("br")
                                else:
                                    doc.stag("br")
                                    text(t("Die Schreibung des Lerners ist umgangssprachlich plausibel"))
                                    doc.stag("br")
                        
                    
                    with tag("span", style="text-align:left;float:left;width:50%"):
                        text(t("richtig wäre:"))
                        doc.stag("br")
                       
                        #mark the characters in the target word which were misspelled in the original             
                        with tag('span', klass="big"):
                            
                            relevant_error_cat_ranges = []
                            
                            for e in relevant_token.geterrors():
                                if e.category == relevant_error_cat and not e.category.startswith("del_"):
                                    relevant_error_cat_ranges.append(e.range)
                            r = 0
                            while r < len(relevant_token.target):
                                found = False
                                for arange in relevant_error_cat_ranges:
                                    if r == arange[0]:                                                              
                                        with tag('span', klass="correct"):
                                            text(relevant_token.target[arange[0]:arange[-1]+1])
                                        r += arange[-1]-arange[0]+1
                                        found = True
                                if found == False:
                                    text(relevant_token.target[r])
                                    r+=1
                        
                        doc.stag("br")
                        doc.stag("br")        
                           
                        #getMaryTTS(relevant_token.target)
                        #with tag("audio", controls="true"):
                        #    doc.stag("source", src=relevant_token.target+".wav", type="audio/wav")
                        #    text("Ihr Browser unterstützt keine Audiowiedergabe.")
                    
                        for e in relevant_token.geterrors():
                            if e.category == relevant_error_cat:
                                if e.morphconst == "neces":
                                    doc.stag("br")
                                    text(t("Die korrekte Schreibung kann von einer verwandten Form hergeleitet werden"))
                                    doc.stag("br")
    
                                
                
                        
        
        outfile = open(outfilename, mode="w", encoding="utf-8")
        print(doc.getvalue(), file=outfile)
        #print(indent(doc.getvalue()))
    
    #error page which shows all errors and correct applications of a particular category 
    #in addition it highlights the current token (more precisely: all tokens with this orig spelling) + says that the category was used correctly here
    #appears when clicking on a specific correct spelling of a category
    def orig_correct_page(relevant_token, relevant_error_cat, outfilename):
        
        doc, tag, text = Doc().tagtext()
        doc.asis('<!DOCTYPE html>')
        with tag('html'):
        
            doc.asis(create_head())
               
            with tag('body'):
                
                with tag("h3"):
                    text(t("Fehler und korrekte Verschriftungen der Kategorie "), relevant_error_cat)
                    
                with tag('p', id="backtomainpage"):
                    doc.asis(link_to_main_page())
                
                with tag('p', id="textfield"):
                    for token in tokens:
                        with tag('span', klass="orig-token"):
                            if token.orig == relevant_token.orig:
                                with tag("span", klass="highlight-token"):
                                    doc.asis(marked_errors_correct(token, relevant_error_cat))
                                text(" ")
                                
                            else:
                                doc.asis(marked_errors_correct(token, relevant_error_cat))
                        
                            with tag("span", klass="target"):
                                text(token.target)
                                
                            #with tag('span', klass="tooltiptext"):
                            #    text("möglicher Fehler:", )
                                
                            
                            
                with tag('div', id="infobox"):
                    text(t("Hier wurde "))
                    with tag("a", style="text-decoration:None;font-weight:bold"):
                        text(relevant_error_cat)
                        doc.attr(href=relevant_error_cat+".html")
                    text(t(" richtig angewandt"))
                
                
                
                        
        
        outfile = open(outfilename, mode="w", encoding="utf-8")
        print(doc.getvalue(), file=outfile)
        #print(indent(doc.getvalue()))
    
    error_page_list = []
    possible_error_page_list = []
    error_orig_page_list = []
    
    
    ############# create html pages
    
    for token in tokens:
        
                        
        for err in token.geterrors():
                
            error_page = err.category+".html"
            if not error_page in error_page_list:
                error_page_list.append(error_page)
                general_error_page(relevant_error_cat=err.category, outfilename=outfolder+"errorpages/"+error_page)
            
                
            error_orig_page = token.orig+"_"+err.category+".html"
            if error_orig_page not in error_orig_page_list:
                error_orig_page_list.append(error_orig_page)
                orig_error_page(relevant_token=token, relevant_error_cat=err.category, outfilename=outfolder+"errorpages/"+error_orig_page)
        
        for pe in possible_errors_to_consider(token):
            
            error_page = pe.category+".html"
            
            if not error_page in error_page_list:
                error_page_list.append(error_page)
                general_error_page(relevant_error_cat=pe.category, outfilename=outfolder+"errorpages/"+error_page)
                
            error_orig_page = "pe_"+token.orig+"_"+pe.category+".html"
            if error_orig_page not in error_orig_page_list:
                error_orig_page_list.append(error_orig_page)
                orig_correct_page(relevant_token=token, relevant_error_cat=pe.category, outfilename=outfolder+"errorpages/"+error_orig_page)
                
    #print(error_orig_page_list)
    
    #add errors to LearnerXML
    xmltoken.errorxml_from_tokenlist(tokens, plain_name+".xml")

    #optional: also create Exmaralda files
    #LearnerXMLToEXMARaLDA_new.learnerxmltoexmaralda(plain_name + ".xml", plain_name + ".exb", prettyprint=True)
    '''

if __name__ == "__main__":
    
    functionality = """This script produces html-pages which highlight and classify orthographic errors in a text that was corrected beforehand"""
    
    parser = argparse.ArgumentParser(description=functionality)
    parser.add_argument('infile', nargs=1, help="the input file; default: csv-file with original token left and target token right separated by a tab (one pair per line).")
    parser.add_argument('--tagset', nargs=1, help="the tagset of error categories to use ('OLFA' or 'Litkey'), default: Litkey")
    parser.add_argument('--outputlang', nargs=1, help="the language of the output information ('de' for German or 'en' for English), default: German")

    args = parser.parse_args()

    tagset = "Litkey" #default
    if args.tagset != None:
        tagset = args.tagset[0]

    outputlang = "de"
    if args.outputlang != None:
        outputlang = args.outputlang[0]

    create_all_html_pages(args.infile[0], tagset, outputlang)
