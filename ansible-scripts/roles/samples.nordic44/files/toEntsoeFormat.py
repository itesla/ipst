#
# Copyright (c) 2016, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

# create the ENTSOE compatible repository structure, also zipping together the _EQ, _SV and _TP files

import re
import sys
from datetime import datetime
import os
import errno
import zipfile

def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise

def checkSourceFiles(origPath):
    for suffix in ["_EQ.xml","_SV.xml","_TP.xml"]:
        if (not os.path.isfile(origPath+suffix)):
            return False
    return True


def zipCimFiles(origPath, targetFolder, basecimfilename):
    destFileName=targetFolder + "/" +basecimfilename + ".zip"
    print origPath + " -> " + destFileName
    zf = zipfile.ZipFile(destFileName, "w", zipfile.ZIP_DEFLATED)
    for suffix in ["_EQ.xml","_SV.xml","_TP.xml"]:
        zf.write(origPath+suffix, arcname=basecimfilename+suffix)
    zf.close()


casetype = "SN"
countrycode = "UX"
version ="0"


if len(sys.argv) < 2:
    print "parameter required: destination folder"
    sys.exit()

rootPath=str(sys.argv[1])
rootPathCases= rootPath + "/CIM/"+ casetype

for line in sys.stdin:
    inputPath=line.rstrip('\n')

    if "noOL" in inputPath:
        continue

    #tokenize inputPath  into an array
    info = re.findall("[a-zA-Z0123456789]+", inputPath)
    #find the date token position
    apos=len(info)-4
    #parse the date
    date1 = datetime.strptime(info[apos], '%Y%m%d')
    #crate subpath from date
    filepath= "%02d/%02d/%02d" % (date1.year,date1.month,date1.day)
    #build the cim filename, entsoe style
    basecimfilename="%04d%02d%02d_%02d%02d_%02s%01d_%02s%01s" % (date1.year, date1.month, date1.day, int(info[apos+3][1:]), 0, casetype,date1.weekday()+1, countrycode, version)

    targetFolder=rootPathCases + "/" + filepath
    if checkSourceFiles(inputPath):
        make_sure_path_exists(targetFolder)
        zipCimFiles(inputPath,targetFolder,basecimfilename)
    else:
        print "ERROR!! cannot create cim file " + basecimfilename +": cannot find a complete files set for input path " + inputPath
