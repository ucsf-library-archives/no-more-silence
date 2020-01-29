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
 * Created by Rebecca Tang on 2/21/19.
 *
 * A quick tool for uploading files or files in a directory to box
 * Note this script does not create directory on box
*/

 @Grab(group='com.box', module='box-java-sdk', version='2.8.2')


import com.box.sdk.*
import groovy.io.FileType;

void uploadOnefile(File fileToUpload, String folderId, BoxAPIConnection client) {

    println("Uploading file ${fileToUpload.getAbsolutePath()}")
    String fileName = fileToUpload.getName()

    // Select Box folder
    BoxFolder folder = new BoxFolder(client, folderId);

    // Upload file
    FileInputStream stream = new FileInputStream(fileToUpload.getAbsolutePath());
    BoxFile.Info newFileInfo = folder.uploadFile(stream, fileName);
    stream.close();
}

def cli = new CliBuilder(usage: 'CheckArtifacts.groovy')

cli.with {

    h longOpt: 'help', 'Show usage information'
    k longOpt: 'devToken', required: true, args: 1, argName: 'devToken', 'Devtoken issued by the BOX developer console'
    f longOpt: 'filePath', required: true, args: 1, argName: 'filePath', 'File to upload'
    t longOpt: 'tagetFolderId', required: true, args: 1, argName: 'targetFolderId', 'Box folder ID to upload to'
}

def options = cli.parse(args)

if (!options) {

    return
}

if (options.h) {

    cli.usage()
    return
}

def devToken = ""
if(options.k) {

    devToken = options.devToken
} else {

    return
}

// Create new basic client with developer token as first argument
BoxAPIConnection client = new BoxAPIConnection(devToken);

// if filepath is a single file, then it will upload that single file
// if filepath is a directory, then it will upload all files under that directory

// Set upload values
String folderId = options.tagetFolderId
String filePath = options.filePath;
File fileToUpload = new File(filePath)

if(!fileToUpload.exists()) {

    println("Could not find the file to upload - $fileToUpload")
    System.exit(0)
}

if (fileToUpload.isFile()) {

    uploadOnefile(fileToUpload, folderId, client)
} else {

    //Note: only uploads what's in this folder, does not recurse
    fileToUpload.eachFile(FileType.FILES) { file ->

        uploadOnefile(file, folderId, client)
    }
}
