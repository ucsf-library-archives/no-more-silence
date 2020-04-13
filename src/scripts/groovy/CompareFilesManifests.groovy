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
 * Created by Rebecca Tang on 3/11/20.
 *
 * Created for the UCSF Archives LSTA project
 * Given two file manifests and compare them, ignoring directory difference
 *
 * This assumes the file manifest is created by:
 * find . -type f -exec openssl md5 {} \;
 *
 * And each line has the format of:
 * > MD5(./ucsf_mss96-01_001_008.pdf)= 3b1193198deff526f43ba2a25856429a
 */


// functions ---------
void TallyFiles(def fileName, def fileTally) {

    String suffix = fileName.substring(fileName.lastIndexOf("."), fileName.length())
    if (fileTally.get(suffix) == null) {

        fileTally.put(suffix, 1)
    } else {

        int count = fileTally.get(suffix)
        fileTally.put(suffix, count + 1)
    }
}


// main logic
//Update origDir and flat Dir here before running script
File file1 = new File("/Users/rtang/Documents/LSTA/nuxeo_downloads_phase2/objects_file_manifest.txt")
File file2 = new File("/Users/rtang/Documents/LSTA/nuxeo_downloads_phase2/objects_flat_file_manifest.txt")

if (!file1.exists()) {

    println("${file1.getPath()} does not exist")
    System.exit(1)
}

if (!file2.exists()) {

    println("${file2.getPath()} does not exist")
    System.exit(1)
}

def file1Counter = 0
def file2Counter = 0
Map file1Md5 = [:]
Map file1Tally = [:]
Map file2Md5 = [:]
Map file2Tally = [:]

file1.each { line ->

    def tokens = line.split("=")
    def fileName = tokens[0].trim().substring(tokens[0].lastIndexOf('/')+1, tokens[0].lastIndexOf(')'))
    def md5 = tokens[1].trim()

    TallyFiles(fileName, file1Tally)
    //println("fileName = $fileName")
    //println("md5 = $md5")
    if (file1Md5.containsKey(fileName)) {

        println("INFO: In ${file1.getName()} - Duplicate file: ${fileName}")
    }
    else {

        file1Md5.put(fileName, md5)
    }
    file1Counter ++
}

println("Total files processed in file ${file1.getPath()} : $file1Counter and file1Md5 size = ${file1Md5.size()}")
println(" Tally:  $file1Tally")
//println(file1Md5)

file2.each { line ->

    def tokens = line.split("=")
    def fileName = tokens[0].substring(tokens[0].lastIndexOf('/')+1, tokens[0].lastIndexOf(')'))
    def md5 = tokens[1].trim()
    TallyFiles(fileName, file2Tally)
    //println("fileName = $fileName")
    //println("md5 = $md5")
    if (file2Md5.containsKey(fileName)) {

        println("INFO: In ${file2.getName()} - Duplicate file: ${fileName}")
    }
    else {

        file2Md5.put(fileName, md5)
    }
    file2Counter ++
}

println("Total files processed in file ${file2.getName()} : $file2Counter and file2Md5 size = ${file2Md5.size()}")
println(" Tally: $file2Tally")
//println(file1Md5)

// key, filename, value is reason
Map differenceMap = [:]

for (entry in file1Md5) {

    if (file2Md5.containsKey(entry.key)) {

        if (file1Md5.get(entry.key) != file2Md5.get(entry.key)) {

            differenceMap.put(entry.key, "md5 value differs in ${file1.getName()} and ${file2.getName()}")
        }
    }
    else {

        differenceMap.put(entry.key, "exists in ${file1.getName()} not in ${file2.getName()}")
    }
}

for (entry in file2Md5) {

    if (file1Md5.containsKey(entry.key)) {

        if (file1Md5.get(entry.key) != file2Md5.get(entry.key)) {

            differenceMap.put(entry.key, "md5 value differs in ${file1.getName()} and ${file2.getName()}")
        }
    }
    else {

        differenceMap.put(entry.key, "exists in ${file2.getName()} not in ${file1.getName()}")
    }
}

println("==> Differences between two files: ")
println(differenceMap)