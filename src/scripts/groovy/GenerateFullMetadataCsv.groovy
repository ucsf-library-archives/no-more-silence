#!/bin/env groovy
/**
 Copyright © 2018, Regents of the University of California
 All rights reserved.
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.
 - Neither the name of the University of California nor the names of its
 contributors may be used to endorse or promote products derived from this
 software without specific prior written permission.
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Created by Rebecca Tang on 1/31/19.
 *
 * Created for the UCSF Archives LSTA project
 *
 * Generates the aggregated csv or tsv metadata file.  Data fields are extracted from .json files
 * ocr text is grabbed from .ocr files
 *
 *
 * csv or tsv file generated based on https://tools.ietf.org/html/rfc4180
 * csv files are separated by a separator, and text are surrounded by qualifiers
 * pipe (|) separated -- update code to change separator and qualifier
 * field values are enclosed in double quotes " "
 * tsv files are separated by tabs, text are surrounded by qualifiers
 * If double quotes appears inside a field, it's escaped by putting anther double quote in front of it: example """The double quote is escaped"""
 *
 * pre-condition: Assuming that the file structure in jsonDir and ocrDir are the same.  If the meta exists in
 * [jsonDir]/path/to/jsonFile, then the ocr file can be fond at ocrDir/path/to/ocrFile
 *
 *
 * The headings are
 * Collection Title                --->    ucldc_schema:source
 * Title                           --->    dc:title
 * Local Identifier 1              --->    ucldc_schema:localidentifier
 * Type                            --->    ucldc_schema:type
 * Date 1                          --->    [ucldc_schema:date][date]
 * Date 1 Type                     --->    [ucldc_schema:date][datetype]
 * Publication/Origination Info 1  --->    ucldc_schema:publisher
 * Creator Name                    --->    [ucldc_schema:creator][name]
 * Creator Name Type               --->    [ucldc_schema:creator][nametype]
 * Creator Source                  --->    [ucldc_schema:creator][source]
 * Format/Physical Description     --->    ucldc_schema:physdesc
 * Language 1                      --->    [ucldc_schema:language][language]
 * Language 1 Code                 --->    [ucldc_schema:language][languagecode]
 * Copyright Status                --->    ucldc_schema:rightsstatus
 * Copyright Statement             --->    ucldc_schema:rightsstatement
 * Source                          --->    ucldc_schema:source
 * Subject (Name) Name             --->    [ucldc_schema:subjectname][name]
 * Subject (Name) Name Type        --->    [ucldc_schema:subjectname][nametype]
 * Subject (Name) Source           --->    [ucldc_schema:subjectname][source]
 * Subject (Topic) Heading         --->    [ucldc_schema:subjecttopic][heading]
 * Subject (Topic) Heading Type    --->    [ucldc_schema:subjecttopic][headingtype]
 * Subject (Topic) Source          --->    [ucldc_schema:subjecttopic][source]
 *
 *
 */

// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
@Grab(group='org.apache.commons', module='commons-lang3', version='3.3.1')
//@Grab(group='commons-lang', module='commons-lang', version='2.6')


import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils


//-------------- functions --------------------//

void recordFieldWithMoreValues(List fields, String name, def data) {
// if field size is 1, then we know there is one value
    if (fields.size() == 2) { //these are multi value fields

        def value = data.properties."${fields.get(0)}"."${fields.get(1)}"
        //println("field = ${fields.get(0)}:${fields.get(1)} --> value = $value")
        def valueSize = 1
        if (value != null) {

            valueSize = value.size()
        }
        if (valueSize > NAME_TO_JSON_VALUES_MAX_COUNT_MAP.get(name)) {

            NAME_TO_JSON_VALUES_MAX_COUNT_MAP.put(name, valueSize)
        }

} else if (fields.size() > 2) {

        println("ERROR: incorrect json field $fields")
    }
}

/*
Cleans up data before writing to file
1) escaping double quotes, i.e. If the data have double quotes, then escape with another double quote
2) change all spaces to one single space, so tabs will be replaced by one white space
3) change all newlines to one white space
 */
String cleanData(String origData) {

    //escape double quotes
    //replace tab with blank space
    if (StringUtils.isBlank(origData)) {

        return origData
    }
    return origData.replaceAll("\"", "\"\"").replaceAll("\\s", " ").replace(System.getProperty("line.separator"), " ").trim()
}

//-------------- main logic -------------------//

def cl = new CliBuilder(usage: "groovy GenerateFullMetadata.groovy -f propertyFile -v")
cl.f(argName: 'propertyFile', longOpt: 'propertyFile', args:1, required: true, 'Property File contains: jsonDir, ocrDir, metadataFile')
cl.v(argName: 'verbose', longOpt: 'verbose', 'Show verbose messages')
cl.h(longOpt:'help', 'Show usage information and quit')


def opt = cl.parse(args)
if (!opt) {

    System.exit(1)
}
def propertiesFile = new File(opt["f"])

if (!propertiesFile.exists()) {

    println("${opt["f"]} does not exist")
    System.exit(1)
}

Properties properties = new Properties()

propertiesFile.withInputStream { it ->

    properties.load(it)
}

File jsonDir = new File(properties."jsonDir")
File ocrDir = new File(properties."ocrDir")
File metadataFileDir = new File(properties.outputDir)
File metadataFile
PrintWriter metadataWriter
def downloadNonPdfCollections = (((properties."downloadNonPdf").trim().replaceAll("%20", " ")).split(";")).toList()


String metadataFileName = properties.metadataFileName
delimiter = properties.delimiter
qualifier = properties.qualifier

println("Delimiter is ${delimiter=="\t"?"Tab":delimiter}, qualifier is ${qualifier}, metadata file name is ${metadataFileName}")


if (jsonDir == null || !jsonDir.exists()) {

    println("Please enter a valid directory of your json files")
    System.exit(1)
}

if (ocrDir == null || !ocrDir.exists()) {

    println("Please enter a valid directory of your ocr files")
    System.exit(1)
}

if (metadataFileDir == null) {

    metadataFile = new File(jsonDir, metadataFileName)
    println("Metadata file is not sepecified, creating metadataFile ${metadataFile.getPath()}")
} else if (!metadataFileDir.exists()) {

    metadataFileDir.mkdirs()
    metadataFile = new File(metadataFileDir, metadataFileName)
    println("Metadata file ${metadataFileDir.getPath()} does not exist, created dir and created file ${metadataFile.getPath()}")
} else {

    metadataFile = new File(metadataFileDir, metadataFileName)
    println("Created metadata file ${metadataFile.getPath()}")
}

metadataWriter = new PrintWriter(metadataFile, "UTF-8");

verbose = opt["v"]?true:false
if (verbose) {

    println("Verbose mode on")
    println("json files are located at ${jsonDir.getPath()}")
    println("ocr files are located at ${ocrDir.getPath()}")
    println("writing to metadata to  ${metadataFile.getPath()}")
}

/*
This map the directories that can be used as collection name
Key is directory name, value is display name
 */
Map<String, String> directoryToCollectionNameMap = [

        "2003-10 Beowulf Thorne Papers" : "Beowulf Thorne Papers, 2003-10",
        "2006-02 Shanti (Project) Records" : "Shanti (Project) Records, 2006-02 ",
        "2006-08 Marin Co. AIDS Task Force and AIDS Advisory Committee Records" : "Marin Co. AIDS Task Force and AIDS Advisory Committee Records, 2006-08",
        "2008-23 Bruce Decker Papers" : "Bruce Decker Papers, 2008-23",
        "AIDS Ephemera" : "AIDS Ephemera",
        "MSS 2000-31 AIDS Ephemera Collection" : "AIDS Ephemera Collection, MSS 2000-31",
        "MSS 94-18 John L. Ziegler, MD papers" : "John L. Ziegler, MD papers, MSS 94-18",
        "MSS 94-28 AIDS Treatment News" : "AIDS Treatment News, MSS 94-28",
        "MSS 94-55 Healing Alternatives Foundation Records" : "Healing Alternatives Foundation Records, MSS 94-55",
        "MSS 96-01 AIDS Legal Referral Panel Files" : "AIDS Legal Referral Panel Files, MSS 96-01 ",
        "MSS 98-39 Conant" : "Conant, MSS 98-39",
        "GLC 45 Vincent Diaries" : "Vincent Diaries, GLC 45",
        "GLC 51 Gary Fisher Papers" : "Gary Fisher Papers, GLC 51",
        "GLC 60 Eric Rofes Papers" : "Eric Rofes Papers, GLC 60",
        "GLC 76" : "GLC 76",
        "SFH 4 San Francisco Department of Public Health AIDS Office records" : "San Francisco Department of Public Health AIDS Office records, SFH 4",
        "2000-46 AIDS Legal Referral Panel Records" : "AIDS Legal Referral Panel Records, 2000-46",
        "2005-13 Sue Rochman Papers" : "Sue Rochman Papers, 2005-13",
        "2003-09 Linda Alband Papers" : "Linda Alband Papers, 2003-09",
        "GLC 63 Barbara Cameron Papers" : "Barbara Cameron Papers, GLC 63",
        "SFH 31 People vs. Owen Bathhouse Closure Litigation" : "People vs. Owen Bathhouse Closure Litigation, SFH 31",
        "SFH 71 Deaf AIDS Center Records" : "Deaf AIDS Center Records, SFH 71",
        "MSS 2001-04 Sally Hughes AIDS Research" : "Sally Hughes AIDS Research, MSS 2001-04",
        "MSS 2009-04 Dritz" : "Dritz, MSS 2009-04",
        "MSS 95-03 Mobilization Against AIDS Records" : "Mobilization Against AIDS Records, MSS 95-03",
        "MSS 95-04 Women's AIDS Network (WAN) Records" : "Women's AIDS Network (WAN) Records, MSS 95-04",
        "MSS 98-47 ACT-UP Golden Gate records" : "ACT-UP Golden Gate records, MSS 98-47",
        "MSS 98-48 Shanti Project Records" : "Shanti Project Records, MSS 98-48",
        "AR 2005-15 CAPS": "UCSF Center for AIDS Prevention Studies (CAPS), 1984-2004, AR 2005-15",
        "AR 2007-14 AIDS Health Project": "UCSF AIDS Health Project Records, 1983-2004, AR 2007-14",
        "MSS 2013-03 Garrett": "Laurie Garrett, 1970-2013, MSS 2013-03",
        "MSS 2015-01 Francis": "Donald Francis, 1970-2005, MSS 2015-1",
        "MSS 93-18 Bay Area HIV Support and Education Services Records": "Bay Area HIV Support and Education Services Records, 1989-1993, MSS 93-18"
]



/*
complex repeating headers repeats by putting the number after the first term
Example1: Creator 1 Name, Creator 1 Name Type, Creator 1 Source, Creator 2 Name, Creator 2 Name Type, Creator 2 Source, etc.
Example2: Language 1, Language 1 Code, Language 2, Language 2 Code, etc.
Example3: Subject (Name) 1 Name, Subject (Name) 1 Name Type, Subject (Name) 1 Source, Subject (Name) 2 Name, etc.
 */
def complexRepeatingHeadersMap = [

        "ComplexGroup Date" : ["Date" : ["", "Type"]],

        "ComplexGroup Creator" : ["Creator" : ["Name", "NameType", "Source"]],

        "ComplexGroup Language" : ["Language" : ["", "Code"]],

        "ComplexGroup Subject (Name)" : ["Subject (Name)" : ["Name", "Name Type", "Source"]],

        "ComplexGroup Subject (Topic)" : ["Subject (Topic)" : ["Heading", "Heading Type", "Source"]]
]
/*
 * map: key is field name
 * value: json field to be extracted from the json file,
 */
def nameToJsonFieldMap = [

        "Collection Title" : ["ucldc_schema:source"],
        "Title" : ["dc:title"],
        "Local Identifier 1" : ["ucldc_schema:localidentifier"],
        "Type" : ["ucldc_schema:type"],

        "ComplexGroup Date" : [
                "Date":["ucldc_schema:date", "date"], //Complex repeating header
                "Date Type":["ucldc_schema:date", "datetype"], //Complex repeating header
        ],

        "Publication/Origination Info" : ["ucldc_schema:publisher"],

        "ComplexGroup Creator" : [
                "Creator Name":["ucldc_schema:creator", "name"], //complex  repeating header
                "Creator Name Type":["ucldc_schema:creator", "nametype"], //complex repeating header
                "Creator Source":["ucldc_schema:creator", "source"], //complex repeating header
                ],

        "Format/Physical Description" : ["ucldc_schema:physdesc"],

        "ComplexGroup Language" : [
                "Language":["ucldc_schema:language", "language"],  //complex repeating header
                "Language Code":["ucldc_schema:language", "languagecode"], //complex repeating header
        ],

        "Copyright Status" : ["ucldc_schema:rightsstatus"],
        "Copyright Statement" : ["ucldc_schema:rightsstatement"],
        "Source" : ["ucldc_schema:source"],

        "ComplexGroup Subject (Name)" : [
                "Subject (Name) Name":["ucldc_schema:subjectname", "name"], //complex repeating header
                "Subject (Name) Name Type":["ucldc_schema:subjectname", "nametype"], //complex repeating header
                "Subject (Name) Source":["ucldc_schema:subjectname", "source"], //complex repeating header
        ],

        "ComplexGroup Subject (Topic)" : [
                "Subject (Topic) Heading":["ucldc_schema:subjecttopic", "heading"], //complex repeating header
                "Subject (Topic) Heading Type":["ucldc_schema:subjecttopic", "headingtype"], //complex repeating header
                "Subject (Topic) Source":["ucldc_schema:subjecttopic", "source"] //complex repeating header
        ]
]

/*
*  Global map
*
*  key is field name
*  value is the max number of values for this field, so that the field can be written out to the metadata file this many times
*
*
*  if multiple, write header as: Creator 1 name, creator 1 name type, creator 1 source, creator 2 name, creator 2 name type, etc
*  Subject (Name) 1 Name , Subject (Name) 1 Name Type , Subject (Name) 1 Source, Subject (Name) 2 Name , Subject (Name) 2 Name Type , Subject (Name) 2 Source ...
*/
NAME_TO_JSON_VALUES_MAX_COUNT_MAP = [

        "Collection Title" : 1,
        "Title":1,
        "Local Identifier" : 1,
        "Type" : 1,
        "ComplexGroup Date" : 1,
        //"Date":1,
        //"Date Type":1,
        "Publication/Origination Info" : 1,
        "ComplexGroup Creator" : 1,
        //"Creator Name":1,
        //"Creator Name Type":1,
        //"Creator Source":1,
        "Format/Physical Description" : 1,
        "ComplexGroup Language" : 1,
        //"Language" : 1,
        //"Language Code" : 1,
        "Copyright Status" : 1,
        "Copyright Statement" : 1,
        "Source" : 1,
        "ComplexGroup Subject (Name)" : 1,
        //"Subject (Name) Name":1,
        //"Subject (Name) Name Type":1,
        //"Subject (Name) Source":1,
        "ComplexGroup Subject (Topic)" : 1,
        //"Subject (Topic) Heading":1,
        //"Subject (Topic) Heading Type":1,
        //"Subject (Topic) Source":1
]


//first pass, field value max size, so that we know how many times to write the repeating headers out
def fileCounter = 0
jsonDir.eachFileRecurse(FileType.FILES) { file ->

    if (file.getName().endsWith("json")) {

        def jsonSlurper = new JsonSlurper()
        def data = jsonSlurper.parseText(file.text)

        if (verbose) println( "Processing metadata file: ${file.getPath()}")
        nameToJsonFieldMap.each { it->

            String name = it.key
            //fields could be
            //                List - non repeats or simple repeats
            //                map  - grouped repeats, find the max number of the group
            def fields = it.value
            if (name.startsWith("ComplexGroup")) {

                ((Map)fields).each { it2 ->

                    List subFields = it2.value

                    recordFieldWithMoreValues(subFields, name, data)
                }

            } else {

                recordFieldWithMoreValues((List)fields, name, data)
            }
        }

        fileCounter++
        if (verbose) println("Processed $fileCounter files")
    }
}
println("Field and max number of values in each field:")
println(NAME_TO_JSON_VALUES_MAX_COUNT_MAP)

/*************
write header
**************/
int headerCounter = 0
NAME_TO_JSON_VALUES_MAX_COUNT_MAP.each { it->

    String header = it.key
    Integer repeat = it.value

    if (repeat == 1) {

        if (header.startsWith("ComplexGroup")) {

            Map headerParts = complexRepeatingHeadersMap.get(header)
            if (headerParts.size() != 1) {

                println("Definition wrong for $header in complexRepeatingHeaderMap")
            } else {

                String key = headerParts.keySet().collect{it}[0]
                List value = headerParts.get(key)
                for (String valueItem : value) {

                    metadataFile << qualifier + (key + " " + valueItem).trim() + qualifier
                    metadataFile << delimiter
                    headerCounter++
                }

            }

        } else {

            metadataFile << qualifier + header + qualifier
            metadataFile << delimiter
            headerCounter++
        }
    } else if (repeat > 1) {

        if (header.startsWith("ComplexGroup")) {

            Map headerParts = complexRepeatingHeadersMap.get(header)
            if (headerParts.size() != 1) {

                println("Definition wrong for $header in complexRepeatingHeaderMap")
            } else {

                String key = headerParts.keySet().collect{it}[0]
                List value = headerParts.get(key)
                for (int i = 0; i < repeat; i++) {

                    for (String valueItem : value) {

                        metadataFile << qualifier + (key + " " + (i + 1) + " " + valueItem).trim() + qualifier
                        metadataFile << delimiter
                        headerCounter++
                    }

                }
            }
        } else { //simple repeating

            for (int i = 0; i < repeat; i++) {

                metadataFile << qualifier + header + " " + (i + 1) + qualifier
                metadataFile << delimiter
                headerCounter++
            }
        }
    } else {

        println("ERROR: incorrect repeat number $repeat")
    }
}
metadataFile << "\"Ocr text\"\n"
headerCounter ++
if (verbose) println("Wrote total $headerCounter headers")

/***********
 write data
 **********/
fileCounter = 0
try {

    jsonDir.eachFileRecurse(FileType.FILES) { file ->

        //if (file.getName().endsWith("70c3cb86-0c49-486d-b113-70b402a9655e.json")) { //spot testing
        //if (file.getName().endsWith("e32ce68e-0b99-4a53-a568-fff14caea05a.json")) {  //test double quote
        if (file.getName().endsWith(".json")) {

            def jsonSlurper = new JsonSlurper()
            def data = jsonSlurper.parseText(file.text)

            if (verbose) println("Processing metadata file: ${file.getPath()}")

            //skip the following if source is empty and title is not.pdf
            boolean writeToCsv = true
            String source = jsonData = data.properties."ucldc_schema:source"
            String title = jsonData = data.properties."dc:title"
            //this means this json file is for the directory and we don't write it out
            if (StringUtils.isBlank(source) && !title.endsWith(".pdf") && !title.endsWith(".tif")) {

                writeToCsv = false
            }

            //create new Source -- which will be written into "collection title" and "source" -- from directory
            String newSource = ""
            File currentDir = file
            if (StringUtils.isBlank(source) && writeToCsv) {

                boolean foundCollection = false

                while (!foundCollection) {

                    println(directoryToCollectionNameMap) //debug
                    println("CurrentDir ${currentDir.getName()}")  //debug
                    if (directoryToCollectionNameMap.containsKey(currentDir.getName())) {

                        newSource = directoryToCollectionNameMap.get(currentDir.getName())
                        foundCollection = true;
                    } else {

                        currentDir = currentDir.getParentFile()
                    }
                    //println("foundCollection $foundCollection") //debug
                }
            }
            println("source = $source && newsource = $newSource") //debug

            //it's a tif file and the collection does not download tif, so we ignore this entry because this entry is not going to be written
            //into datafile
            if (title.endsWith(".tif") && !downloadNonPdfCollections.contains(currentDir.getName())) {

                writeToCsv = false;
                println("Don't need to write: $newSource and $title")
            }

            if (title.endsWith(".tif")) { //debug

                println("title: $title && writeTocsv? : $writeToCsv")
            }

            if (writeToCsv) {

                nameToJsonFieldMap.each { it ->

                    String name = it.key
                    def fields = it.value

                    if (name.startsWith("ComplexGroup")) {

                        //build values into List of List
                        List<List> jsonDataList = []
                        ((Map) fields).each { it2 ->

                            List subFields = it2.value
                            if (subFields.size() == 2) {

                                List jsonData = data.properties."${subFields.get(0)}"."${subFields.get(1)}"
                                //println("field = ${subFields.get(0)}:${subFields.get(1)} --> jsonData = $jsonData") //debug
                                jsonDataList.add(jsonData)
                            } else {

                                println("ERROR: complex fields jason fields incorrect $subFields")
                            }
                        }
                        int maxRepeatSize = NAME_TO_JSON_VALUES_MAX_COUNT_MAP.get(name)
                        int actualRepeatSize = jsonDataList.get(0).size()
                        for (int i = 0; i < maxRepeatSize; i++) {

                            jsonDataList.each { list ->

                                if (i < list.size()) {

                                    String jsonDataItem = list.get(i)
                                    if (jsonDataItem == null) jsonDataItem = " "

                                    jsonDataItem = cleanData(jsonDataItem)
                                    metadataFile << qualifier + jsonDataItem + qualifier
                                    //surround values by double quotes
                                    metadataFile << delimiter

                                    //println("  " + qualifier + jsonDataItem + qualifier)  //debug
                                } else {

                                    metadataFile << qualifier + " " + qualifier //surround values by qualifiers
                                    metadataFile << delimiter

                                    //println("  " + qualifier + " " + qualifier)  //debug
                                }
                            }

                        }

                    } else {  //single value or simple repeats

                        def jsonData
                        if (((List) fields).size() == 1) {

                            jsonData = data.properties."${fields.get(0)}"
                            //println("field = ${fields.get(0)} --> value = $jsonData") //debug
                            if (jsonData == null) jsonData = " "

                        } else if (((List) fields).size() == 2) {

                            jsonData = data.properties."${fields.get(0)}"."${fields.get(1)}"
                            //println("field = ${fields.get(0)}:${fields.get(1)} --> jsonData = $jsonData") //debug

                        } else {

                            println("ERROR: incorrect json field")
                        }

                        if (jsonData instanceof String) {

                            if (jsonData == null) jsonData = " "

                            if (name.equalsIgnoreCase("Collection Title") || name.equalsIgnoreCase("Source")) {

                                //println("Name = $name") //debug
                                //println("jsonData before $jsonData") //debug
                                if (StringUtils.isBlank(jsonData)) jsonData = newSource
                                //println("jsonData after $jsonData")
                            }

                            jsonData = cleanData(jsonData)
                            //if (name == "Type") println("name: type, value $jsonData") //debug
                            //replace return with blank space
                            metadataFile << qualifier + jsonData + qualifier //surround values by text qualifier
                            metadataFile << delimiter
                            //println("  " + qualifier + jsonData + qualifier) //debug
                        } else if (jsonData instanceof List) {

                            if (jsonData.isEmpty()) {

                                metadataFile << qualifier + " " + qualifier //surround values by double quotes
                                metadataFile << delimiter
                                //println("  " + qualifier + " " + qualifier) //debug
                            } else {

                                for (String jsonDataItem : jsonData) {

                                    if (jsonDataItem == null) jsonDataItem = " "

                                    jsonDataItem = cleanData(jsonDataItem)
                                    //replace return with blank space
                                    metadataFile << qualifier + jsonDataItem + qualifier
                                    //surround values by double quotes
                                    metadataFile << delimiter
                                    //println("  " + qualifier + jsonDataItem + qualifier) //debug
                                }
                            }
                        } else {

                            println("ERRROR: value is ${jsonData.getClass()} - unexpected type")
                        }
                    }

                }


                File ocrFileDir = ocrDir
                if (!ocrDir.getAbsolutePath().endsWith("_flat")) { // not flat, so we might mirror

                    String absolutePath = file.getAbsolutePath()
                    String relativeFilePath = absolutePath.substring(jsonDir.getAbsolutePath().length(), absolutePath.lastIndexOf(File.separator));
                    ocrFileDir = new File(ocrDir, relativeFilePath)
                }
                //def ocrFileName = data.properties."ucldc_schema:localidentifier"
                String ocrFileText = " "
                if (data.properties."file:content" != null) {

                    String ocrFileName = data.properties."file:content"."name"
                    println("file:content.name = $ocrFileName")
                    if (ocrFileName.endsWith(".pdf") || ocrFileName.endsWith(".tif")) {

                        if (ocrFileName.endsWith(".pdf")) {

                            ocrFileName = ocrFileName.replace(".pdf", ".ocr")
                        } else {

                            ocrFileName = ocrFileName.replace(".tif", ".ocr")
                        }

                        println("ocrfile => $ocrFileName")
                        File ocrFile = new File(ocrFileDir, ocrFileName)

                        if (!ocrFile.exists()) {

                            println("WARNNING $ocrFile does not exist")
                        } else {

                            ocrFileText = cleanData(ocrFile.getText("UTF-8"))
                        }
                    }
                } else {

                    println("INFO: does not have have file:content")
                }
                metadataFile << qualifier + ocrFileText + qualifier
                metadataFile << "\n"
            }

            fileCounter++
            if (verbose) println("Processed $fileCounter files")
        }
    }
} catch (Exception e) {

    throw (e)
} finally {

    if (metadataWriter != null) {

        metadataWriter.close()
    }
}


if (verbose) println("Total json files processed $fileCounter")
if (verbose) println("Done")
System.exit(0)