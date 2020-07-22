#!/usr/bin/python
# -*- coding: utf-8 -*-


from marcel_levenshtein.WeightedLevenshtein import LevenshteinWeights
from marcel_levenshtein.Marcel_Levenshtein import LevenshteinAligner

import datetime

class MainApplication(object):
    args = None
    weights = None

    def __init__(self, word1, word2, param, type="xml", encoding="utf-8", style="verbose", nonid=False):
        self.word1 = word1
        self.word2 = word2
        self.encoding = encoding
        self.style = style
        self.nonid = nonid
        
        if param:
            self.weights = LevenshteinWeights(param, type)
        else:
            self.weights = LevenshteinWeights()
        

    def run(self):
        aligner = LevenshteinAligner(weights=self.weights)
        style = self.style
        nonid = self.nonid
        
        #for line in self.infile:
        #    try:
        #        (word1, word2) = line.strip().split('\t')
        #    except ValueError:
        #        print("*** Ignoring line: %s" % line)#, file=sys.stderr)

            #if not (nonid and word1 == word2):
        return aligner.bio_align(self.word1, self.word2)
    
def get_alignment(word1, word2, param, type="xml", encoding="utf-8", style="verbose", nonid=False):
    
    #print("init weights",datetime.datetime.now())
    if param:
        weights = LevenshteinWeights(param, type)
    else:
        weights = LevenshteinWeights()
    #print("weights done", datetime.datetime.now())
    
    aligner = LevenshteinAligner(weights=weights)
    #style = self.style
    #nonid = self.nonid
        
        #for line in self.infile:
        #    try:
        #        (word1, word2) = line.strip().split('\t')
        #    except ValueError:
        #        print("*** Ignoring line: %s" % line)#, file=sys.stderr)

            #if not (nonid and word1 == word2):
    return aligner.bio_align(word1, word2)
    
#if __name__ == '__main__':
#description = "Takes an XML file containing Marcel_Levenshtein weights and prints character alignments for given word pairs."
#epilog = ""
#parser = argparse.ArgumentParser(description=description, epilog=epilog)
#parser.add_argument('infile',
#                    nargs='?',
#                    type=argparse.FileType('r'),
#                    default=sys.stdin,
#                    help='File containing word pairs to bio_align, tab-separated (default: <STDIN>)')
#parser.add_argument('-e', '--encoding',
#                    default='utf-8',
#                    help='Encoding of the input file (default: utf-8)')
#parser.add_argument('-f', '--file',
#                    dest="param",
#                    type=str,
#                    help='Parameter file')
#parser.add_argument('-t', '--type',
#                    choices=['tabbed','xml'],
#                    default='xml',
#                    help='Parameter file format (default: %(default)s)')
#parser.add_argument('-s', '--style',
#                    choices=['linear','verbose'],
#                    default='verbose',
#                    help='Output style (default: %(default)s)')
#parser.add_argument('-n', '--non-identical',
#                    dest="nonid",
#                    action='store_true',
#                    default=False,
#                    help='Only print alignments for non-identical word pairs')
#
#args = parser.parse_args()

# launching application ...
#MainApplication(args).run()
