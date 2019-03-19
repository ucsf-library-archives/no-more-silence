import groovy.io.FileType
import groovy.json.JsonSlurper

/**
Copyright © 2019, Regents of the University of California
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
 * Created by Rebecca Tang on 1/11/19.
 *
 * Created for the UCSF Archives LSTA project
 * download metadata (.json) from Nuxeo
 * call nxls to download
 *
 * In order to run this script pynux and nxls must be installed and configured:
 * https://github.com/ucldc/pynux
 *    Note: the values given in the doc needs to be upated to:
 *      base = https://nuxeo.cdlib.org/Nuxeo/site/api/v1
 *      X-NXDocumentProperties = *
 * https://github.com/ucldc/nuxeo_spreadsheet/wiki
 *
 * Then add the path to python path in .bashrc (linux) or .bash_profile(mac)
 * export PYTHONPATH="${PYTHONPATH}:/my/pynux/path"
 * for example, on my machine: export PYTHONPATH="${PYTHONPATH}:/Users/rtang/anaconda/lib/python2.7/site-packages/pynux"
 *
 * Example DownloadNuxeoMetadata.properties
 * downloadPath= AR 2005-15 CAPS;GLBTHS;GLBTHS/AIDS Ephemera
 * downloadDir=/Users/rtang/nuxeo_downloads/json
 */


//--------------- functions -------------

def downloadMetadataFromNuxeo(def downloadPathBase, def path, def downloadDir, def verbose) {

    File downloadSubDir = new File(downloadDir, path)
    if (!downloadSubDir.exists()) {

        downloadSubDir.mkdirs()
        if (verbose) {

            println("Creating dir for ${downloadSubDir.getPath()}")
        }
    }

    ProcessBuilder nxlsCommand = new ProcessBuilder("nxls", "${downloadPathBase + path}", "--outdir", "${downloadSubDir.getPath()}")

    def out = new StringBuffer()
    def err = new StringBuffer()

    def nxls = nxlsCommand.start() //nxlsCommand.execute()
    nxls.consumeProcessOutput(out, err)
    nxls.waitFor()

    if (out.size() > 0) println out
    if (err.size() > 0) println err

    if (nxls.exitValue()) {

        print "Error Downloading metadata for $path: exit value ${nxls.exitValue()}"

    } else {

        println(" Successfully downloaded metadata for $path")
    }

    // Go through files and look for complex objects, if so create sub-folder and download json
    downloadSubDir.eachFileRecurse(FileType.FILES) { file ->

        if (file.getName().endsWith(".json")) { //download PDFs

            def jsonSlurper = new JsonSlurper()
            def data = jsonSlurper.parseText(file.text)

            if (verbose) println("  Checking metadata file: ${file.getPath()}")


            if (data.properties."file:content" == null) {

                String pathFromMeta = data.path
                println("    pathFromMeta = $pathFromMeta, path = $path")
                if (!pathFromMeta.endsWith(path)) {

                    println("    Download this complex object")
                    String newPath = pathFromMeta.substring(downloadPathBase.length())
                    println("    newpath = $newPath")

                    downloadMetadataFromNuxeo(downloadPathBase, newPath, downloadDir, verbose)
                }
            }
        }
    }
}

//-------------- main logic -------------

def cl = new CliBuilder(usage: "groovy DownloadNuxeoMetadata.groovy -f propertyFile")
cl.f(argName: 'propertyFile', longOpt: 'propertyFile', args:1, required: true, 'Property File, nuxeo download path(s) - semicolon separated, download dir')
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

def downloadPath = properties."downloadPath"
def paths = downloadPath.split(";")
def downloadDir = new File(properties."downloadDir")

verbose = opt["v"]?true:false
if (verbose) {

    println("Verbose mode on")
    println("Download path -> $downloadPath")
}

println("download to directory = ${downloadDir.getPath()}")
if (!downloadDir.exists()) {

    downloadDir.mkdirs()
    if (verbose) {

        println("Download to dir doesn't exit, creating...")
    }
}


def downloadPathBase = "/asset-library/UCSF/"
for (String path: paths) {

    path = path.trim()
    println ("  path -> $path")

    downloadMetadataFromNuxeo(downloadPathBase, path, downloadDir, verbose)

}
