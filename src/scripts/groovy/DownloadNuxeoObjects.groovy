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
 * Created by Rebecca Tang on 11/20/18.
 *
 * Created for the UCSF Archives LSTA project
 * download objects from Nuxeo
 * loop through metadata files (.json) in each dir and download each object based on url using basic auth ucsf_readonly
 * The file path is stored in "data" field and the 2nd occurrence of nuxeo needs to be replaced as Nuxeo in order to use basic auth
 *
 *
 * Example property file:
 * metadataDir=[path]/nuxeo_downloads/json
 * username=[basic auth user name]
 * password=[basic auth password]
 * downloadDir=[path]/nuxeo_downloads/objects
 * downloadNonPdf=GLBTHS/AIDS Ephemera;GLBTHS;[another collection - provide full path after /asset-library/[Institute]/]];[another collection]
 */

import groovy.io.FileType
import groovy.json.JsonSlurper



//---------------- functions ------------
def downloadFile(boolean verbose, File downloadDir, String subDir, String downloadUrl, String username, String password) {

    def basicAuthUrl = downloadUrl.replace("/nuxeo/", "/Nuxeo/")
    println("          basicAuthUrl + $basicAuthUrl")

    def fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/")+1, downloadUrl.length())
    if (verbose) println("          Download to File Name $fileName")

    def subDownloadDir = new File(downloadDir, subDir)
    if (!subDownloadDir.exists()) {

        subDownloadDir.mkdirs()
        if (verbose) println("Creating dir $subDownloadDir")
    }

    File downloadFile = new File(subDownloadDir, fileName)
    if (downloadFile.exists()) {

        println("${downloadFile.getAbsolutePath()} exists, skipping")
    } else {

        downloadFile.withOutputStream { out ->

            def url = new URL(basicAuthUrl).openConnection()
            println("          Downloading url => $url")
            def remoteAuth = "Basic " + "${username}:${password}".bytes.encodeBase64()
            url.setRequestProperty("Authorization", remoteAuth);
            out << url.inputStream
            FILE_COUNTER++
            if (verbose) println "  File(s) downloaded  $FILE_COUNTER"

        }
    }
}

//-------------- main logic -------------

def cl = new CliBuilder(usage: "groovy DownloadNuxeoObjects.groovy.groovy -f propertyFile")
cl.f(argName: 'propertyFile', longOpt: 'propertyFile', args:1, required: true, 'Property File, holding metadata file full path and Basic Auth username/password')
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

def metadataDir = new File(properties."metadataDir")
def username = properties."username"
def password = properties."password"
def downloadDir = new File(properties."downloadDir")
def downloadNonPdfCollections = ((properties."downloadNonPdf").split(";")).toList()

verbose = opt["v"]?true:false
if (verbose) {

    println("Verbose mode on")
    println("username = $username")
    println("password = $password")
    println("Download non-pdfs from these collections: $downloadNonPdfCollections")
}

println("metadata directory = ${metadataDir.getAbsolutePath()}")
if (!metadataDir.exists()) {

    println("metadata diretory ${metadataDir.getAbsolutePath()} does not exist")
    System.exit(1)
}

if (!downloadDir.exists()) {

    downloadDir.mkdirs()
    if (verbose) println("$downloadDir does not exist, creating")
}

FILE_COUNTER = 0
metadataDir.eachFileRecurse (FileType.FILES) { file ->

    if (file.getName().endsWith(".json")) { //download PDFs

        def jsonSlurper = new JsonSlurper()
        def data = jsonSlurper.parseText(file.text)

        if (verbose) println( "Downloading based on metadata file: ${file.getAbsolutePath()}")

        def subDir = file.getAbsolutePath().substring(downloadDir.getAbsolutePath().length()-2, file.getAbsolutePath().length()-file.getName().length()-1)
        println("subdir $subDir")
        boolean downloadNonPdf = false // default is false, only for a few collections we need to download tiffs
        for (String collection: downloadNonPdfCollections) {

            if (subDir.contains(collection)) {

                downloadNonPdf = true
            }
        }

        if (verbose) println("For collection $subDir, downloadNonPdf is $downloadNonPdf")

        if (data.properties."file:content" != null) {

            def mainFileUrl = data.properties."file:content"."data"
            if (verbose) println("   main file " + mainFileUrl)

            if (downloadNonPdf) {

                downloadFile(verbose, downloadDir, subDir, mainFileUrl, username, password)
            } else { //pdf only

                if (mainFileUrl.endsWith(".pdf")) {

                    downloadFile(verbose, downloadDir, subDir, mainFileUrl, username, password)
                }
            }
        }

        data.properties."files:files".each {

            if (verbose) println("          attachments: " + it["file"]["data"])

            def attachmentUrl = it["file"]["data"]
            if (downloadNonPdf) {

                downloadFile(verbose, downloadDir, subDir, attachmentUrl, username, password)
            } else { //pdf only

                if (attachmentUrl.endsWith(".pdf")) {

                    downloadFile(verbose, downloadDir, subDir, attachmentUrl, username, password)
                }
            }
        }
    }
}

if (verbose) println("Total files downloaded $FILE_COUNTER")




