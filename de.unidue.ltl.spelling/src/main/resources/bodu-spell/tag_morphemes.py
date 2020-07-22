'''
Created on 21.06.2017

@author: Katrin
'''
from tagger import tag_list

def map_morphemetag(tag):
    #Map specific stts tags to the corresponding morpheme tags.
    if tag in ["ADJA", "ADJD"]:
        tag = "ADJ"
    elif tag in ["APPR", "APPO", "APZR", "APPRART"]:
        tag = "ADP"
    elif tag in ["PDS", "PDAT"]:
        tag = "PD"
    elif tag in ["PPOSS", "PPOSAT"]:
        tag = "PPOS"
    elif tag in ["PRELS", "PRELAT"]:
        tag = "PREL"
    elif tag in ["PWAV", "PWAT", "PWS"]:
        tag = "PW"
    elif tag in ["PIAT", "PIDAT", "PIS"]:
        tag = "PI"
    elif tag.startswith("V"):
        tag = "V"
    return tag

def map_stts_to_morphemes(tagged_text):
    
    #Create a list to save the text with morpheme tags.
    tagged_morphemes = list()
    
    #Go through the stts tagged text
    #[(word, tag), ...]
    for i in range(len(tagged_text)):
        
        #Select each word and its corresponding stts tag.
        word = tagged_text[i][0]
        tag = tagged_text[i][1]

        #Map specific stts tags to the corresponding morpheme tags.
        if tag in ["ADJA", "ADJD"]:
            tag = "ADJ"
        elif tag in ["APPR", "APPO", "APZR", "APPRART"]:
            tag = "ADP"
        elif tag in ["PDS", "PDAT"]:
            tag = "PD"
        elif tag in ["PPOSS", "PPOSAT"]:
            tag = "PPOS"
        elif tag in ["PRELS", "PRELAT"]:
            tag = "PREL"
        elif tag in ["PWAV", "PWAT", "PWS"]:
            tag = "PW"
        elif tag in ["PIAT", "PIDAT", "PIS"]:
            tag = "PI"
        elif tag.startswith("V"):
            tag = "V"

        #Append a tuple (word, morphTag) to the result list.
        tagged_morphemes.append((word, tag))
        
    #Return the tagged text.
    return tagged_morphemes

###################################################

def tag_text_with_morpheme_tags(text, tagger_type="kids", caseInsensitive=False):

    #Tag the given text with stts tags.
    tagged_text = tag_list(text, tagger_type, caseInsensitive, correct_format=True)

    #Map the tags to morpheme tags.
    tagged_text = map_stts_to_morphemes(tagged_text)
    
    #Return the tagged text.
    return tagged_text

###################################
###################################
###################################
if __name__ == '__main__':
    print(tag_list(["Das", "ist", "ein", "einfacher", "Test", "zum", "Ausprobieren"], tagger_type="kids", correct_format=True))
    print(tag_text_with_morpheme_tags(["Das", "ist", "ein", "einfacher", "Test", "zum", "Ausprobieren"]))
