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
 * Created by Rebecca Tang on 2/20/19.
 *
 * quick script to flatten and take tally of file directory
 * This script is not as polished as other scripts
 *
 * sourceDir is a directory that has sub directories
 * flatDir is the directory to copy all the files from the sourceDir to, not preserving sub directory structure
 * It will print out at the end how many files are copied.  The types of files that are copied and how many for each type.
 */

import groovy.io.FileType

//Update origDir and flat Dir here before running script
File sourceDir = new File("/Users/rtang/nuxeo_downloads/objects")
File flatDir = new File("/Users/rtang/nuxeo_downloads/objects_flat_2")

if (!flatDir.exists()) {

    flatDir.mkdirs()
}

def fileCounter = 0
Map fileTally = [:]
sourceDir.eachFileRecurse (FileType.FILES) { file ->

    String name = file.getName()
    String suffix = name.substring(name.lastIndexOf("."), name.length())
    if (fileTally.get(suffix) == null) {

        fileTally.put(suffix, 1)
    } else {

        int count = fileTally.get(suffix)
        fileTally.put(suffix, count+1)
    }
    println("copy command: cp ${file.getAbsolutePath()} ${flatDir.getAbsolutePath()}/$name")

    ProcessBuilder cpCommand = new ProcessBuilder("cp", "${file.getAbsolutePath()}", "${flatDir.getAbsolutePath()}/$name")

    def out = new StringBuffer()
    def err = new StringBuffer()

    def cp = cpCommand.start() //nxlsCommand.execute()
    cp.consumeProcessOutput(out, err)
    cp.waitFor()

    if (out.size() > 0) println out
    if (err.size() > 0) println err

    fileCounter ++
}


println("Total files copied: $fileCounter")
println(fileTally)