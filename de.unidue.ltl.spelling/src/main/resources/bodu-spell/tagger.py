'''
Created on 13.05.2017

@author: Katrin
'''
import os
import argparse
from nltk import StanfordPOSTagger as tagger

TAGGER = None

########################################################

def load_tagger(tType="standard", caseInsensitive=False):
    #Import Stanford tagger.
    global TAGGER
    
    #Tagger for standard German.
    if tType == "standard":
        TAGGER = tagger("./tagger/german-hgc_tiger_tueba.tagger", "./tagger/stanford-postagger-3.6.0.jar")
    #hgc-Tagger for German.
    elif tType == "hgc":
        TAGGER = tagger("./tagger/german-hgc.tagger", "./tagger/stanford-postagger-3.6.0.jar")
    #Special tagger for childrens' texts.
    elif tType == "kids" and not caseInsensitive:
        TAGGER = tagger("./tagger/german-bidirectional_kinder.tagger", "./tagger/stanford-postagger-3.6.0.jar")
    #Special tagger for childrens' texts ignoring lettercase.
    elif tType == "kids" and caseInsensitive:
        TAGGER = tagger("./tagger/german-bidirectional-caseless_kinder.tagger", "./tagger/stanford-postagger-3.6.0.jar")
    #No legal tagger type given.
    else:
        print("Failed to load tagger. Tagger type needs to be one of 'standard', 'hgc', 'kids'.")

########################################################

def tag_file(infile):
    try:
        text = [line.strip() for line in infile if line != "\n"]
    
        tagged_text = TAGGER.tag(text)
    
        return tagged_text
    
    except:
        return None

########################################################

def correct_output_format(tagged_list, tagger_type="standard"):
    corrected_list = list()
    if tagger_type in ["standard", "kids"]:
        for i in range(0, len(tagged_list), 2):
            corrected_list.append((tagged_list[i][1], tagged_list[i+1][1]))
        return corrected_list
    return tagged_list

########################################################

def tag_list(tokenlist, tagger_type="standard", caseInsensitive=False, correct_format=False):
    try:
        if not TAGGER:
            load_tagger(tagger_type, caseInsensitive)
        tagged_list = TAGGER.tag(tokenlist)
        if correct_format:
            tagged_list = correct_output_format(tagged_list, tagger_type=tagger_type)
        return tagged_list
    except:
        return None

########################################################

def print_result(tagged_text, filename, output_directory, tagger_type):
    outfile = open(output_directory+"/"+filename.split(".")[0]+"_tagged.csv", mode="w", encoding="utf-8")

    if tagger_type in ["standard", "kids"]:
        for i in range(0, len(tagged_text), 2):
            print(tagged_text[i][1], tagged_text[i+1][1], sep="\t", file=outfile)
            
    else:
        for line in tagged_text:
            print("\t".join(line), file=outfile)
        
    outfile.close()

########################################################

if __name__ == '__main__':
    functionality = """This is an interface for using the StanfordPOSTagger with models trained for different text types."""
     
    parser = argparse.ArgumentParser(description=functionality)
    parser.add_argument("-in", "--input_directory", help='the directory, where the input files with one word per line are stored')
    parser.add_argument("-out", "--output_directory", help='directory, where the tagged files are saved')
    parser.add_argument("-t", "--tagger", help="tagger model that shall be used; can be 'standard', 'hgc' or 'kids'")
    parser.add_argument("-c", "--case_sensitive", help="whether or not lettercase shall be considered; default = True; only relevant if an according tagger model exists")
    parser.add_argument("-o", "--overwrite", help="whether or not files in the output directory shall be overwritten; default = True")
    args = parser.parse_args()
    
    #Check if input directory exists and contains files.
    if not args.input_directory:
        args.input_directory = ""
    if not os.path.isdir(args.input_directory):
        print(args.input_directory, "is not a directory.")
        exit
    elif not os.listdir(args.input_directory):
        print("No files in", args.input_directory)
        exit
        
    #Try to create output directory if it does not exist.
    if not args.output_directory:
        args.out_directory = ""
    if not os.path.isdir(args.output_directory):
        try:
            os.mkdir(args.output_directory)
        except:
            print("Output directory", args.out_directory, "could not be created.")
            exit
       
    if args.case_sensitive == "False":
        args.case_sensitive = False
    else:
        args.case_sensitive = True
    
    if args.overwrite == "False":
        args.overwrite = False
    else:
        args.overwrite = True
        
    #Try to load the tagger.
    load_tagger(args.tagger, args.case_sensitive)
    
    #If tagger was successfully loaded.
    if TAGGER:
        
        #Go through list of input texts and try to tag them.
        for file in os.listdir(args.input_directory):
            
            #If file was not tagged yet or tagged file shall be overwritten:
            if args.overwrite or (not args.overwrite and not file[:-4]+"_tagged.csv" in os.listdir(args.output_directory)):
                
                try:
                    #Open file.
                    infile = open(args.input_directory+"/"+file, mode="r", encoding="utf-8")
                    
                    #Tag text.
                    tagged_text = tag_file(infile)
                    
                    if tagged_text:
                        #Print the result.
                        print_result(tagged_text, file, args.output_directory, args.tagger)
                        
                        #Close the file.
                        infile.close()
                    
                    else:
                        print("Failed to tag", file)
                except:
                    print("Failed to tag", file)
            else:
                print("Skipped file", file)
