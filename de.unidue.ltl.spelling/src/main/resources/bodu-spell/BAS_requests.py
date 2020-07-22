#coding=utf-8
'''
Created on 17.06.2015

Access to BAS webservice.

@author: Ronja Laarmann-Quante
'''

import os
import requests
import xml.etree.ElementTree as ET

def getBAS(infile, iform="list"):
    
    if iform != None and iform != "list" and iform != "txt":
        iform = "list"
        print("Invalid input format for BAS specified! Allowed are 'list' for a list of words or 'txt' (coherent text). The results are now computed with the format 'list'" )
    
    #upload text to BAS webservice G2P
    url = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/services/runG2P"
    file = {"i": open(infile, encoding="utf-8")}
    postdata = {"com":"no",
                "align":"no", 
                "stress":"yes", # new 
                "lng":"deu-DE",
                "syl":"yes", # new 
                "embed":"no",
                "iform": iform, #"list" vs. "txt"
                "nrm":"no",
                "oform":"exttab",
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
    

def getSingleBAS(single_word):
    print(single_word, file=open("temporary.txt", mode= "w", encoding="utf-8"))
    target = getBAS("temporary.txt", iform ="list")
    os.remove("temporary.txt")
    return target.strip()
