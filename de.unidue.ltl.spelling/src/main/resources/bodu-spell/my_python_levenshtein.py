#coding=utf-8
'''
Created on 14.04.2015

Extensions to pyhton-Levenshtein

@author: Ronja Laarmann-Quante
'''

import Levenshtein #python-levenshtein
import re

#print(Levenshtein.editops("Haus", "Mauke"))


#customize output of edit operations of python-levenshtein
def get_operations(source, target):
    lev = Levenshtein.editops(source.lower(), target.lower())
    mylev = []
    
    for (operation, source_index, target_index) in lev:
        if operation == "insert":
            mylev.append((operation, source_index, target[target_index]))
        elif operation == "delete":
            mylev.append((operation, source_index, source[source_index]))
        else: #replace
            mylev.append((operation, source_index, source[source_index], target[target_index]))
    
    return mylev

#print(get_operations("dangge", "danke"))
#print(get_operations("ab", "acbde"))


#use knowledge about edit operations to align two strings and return the correspondences of their indices
#e.g. "Farat" and "Fahrrad" = [[[0, 0], [0, 0]], [[1, 1], [1, 1]], [['<eps>', '<eps>'], [2, 2]], [['<eps>', '<eps>'], [3, 3]], [[2, 2], [4, 4]], [[3, 3], [5, 5]], [[4, 4], [6, 6]]]
def align_levenshtein(source, target):
    operations = get_operations(source, target)
    longer = max(len(source), len(target))
    source_position = 0
    source_counter = 0
    target_counter = 0
    
    aligned = []
    
    if len(operations) == 0: #if source and target identical
        aligned = [[[i,i],[i,i]] for i in range(len(source))]
        #print(aligned)
        return aligned
    
    ops = False
    while source_position < len(source):

        for op in operations:
            if op[0] == 'delete' and op[1] == source_position:
                aligned.append((source_counter, "<eps>"))
                source_counter += 1
                ops = True
                
                
            elif op[0] == 'insert' and op[1] == source_position:
                aligned.append(("<eps>", target_counter))
                target_counter += 1
                ops = 'insert'
                
                
            elif op[0] == 'replace' and op[1] == source_position:
                aligned.append((source_counter,target_counter))
                source_counter += 1
                target_counter += 1
                ops = True
            
            
        if ops != True:
            aligned.append((source_counter,target_counter))
            source_counter += 1
            target_counter += 1
    
        source_position += 1
        ops = False
                    
    for op in operations: #insertions at the end of a word
        if op[0] == 'insert' and op[1] == len(source):
            aligned.append(("<eps>", target_counter))
            target_counter += 1
            
    aligned = [[[o,o],[t,t]] for (o,t) in aligned] 
    #print(operations)
    #print(aligned)
    return aligned

#return the aligned letters of two strings as tuples
def align_levenshtein_letters(source, target):
    aligned_indices = align_levenshtein(source, target)
    aligned_letters = []
    for pair in aligned_indices:
        if pair[0] == ['<eps>', '<eps>']:
            aligned_letters.append(("<eps>", target[pair[1][0]:pair[1][1]+1] ))
        elif pair[1] == ['<eps>', '<eps>']:
            aligned_letters.append((source[pair[0][0]:pair[0][1]+1],"<eps>"))
        else:
            aligned_letters.append((source[pair[0][0]:pair[0][1]+1], target[pair[1][0]:pair[1][1]+1]))
    return aligned_letters


### combine errors of a single pcu to a candidate
def combine_errors_single_pcu(target_pcu, pcu1, pcu2):
    pcu_string = target_pcu
    sub = [pcu1, pcu2]
    double = False
    
    pcu = []
    for c in pcu_string:
        pcu.append([c])
    #print(pcu)
    
    ops =[]
    for s in sub:
        if s.lower() == pcu_string.lower() + pcu_string.lower(): # character dobuling is handled separately
            double = True
            continue
        ops += get_operations(pcu_string, s) 
        
    #print(ops)
    
    for op in ops:
        if op[0] == "delete":
            pcu[op[1]] =[]
        elif op[0] == "insert":
            if op[1] > len(pcu)-1: pcu.append([op[2]]) #at end of word
            else: pcu[op[1]].append(op[2])
        elif op[0] == "replace":
            pcu[op[1]] = [op[3]]
    #print(pcu)
    
    # e.g. <Bett> has <t> and <dd> as candidates for <tt> and its combination would be <dd> again; this reduces it to <d>
    if len(pcu) == 2 and pcu[0] == pcu[1]:
        pcu = pcu[0]  

         
    candidate = "".join("".join(l) for l in pcu)
    
    if double == True: # if doubling plays a role, double the character to get the candidate
        candidate += candidate.lower()
        if candidate == "kk":
            candidate = "ck"
            
    if candidate == "nkg": #result of "ngg" and "nk" but it should be "nck"
        candidate = "nck"
        
    #print(candidate)
    return candidate

#print(combine_errors_single_pcu("tt", "t", "dd"))
#print(align_levenshtein_letters("Lea wil dodo mit nehmen . ", "lea will dodo mitnehmen . "))


#print(Levenshtein.distance("steht", "sthet"))
