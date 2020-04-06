#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR
source venv/bin/activate
# $0 contains full path to file that should be processed
# $1 contains language, "de" or "en"
python3 html_newpagecreator2.py $1 --outputlang $2