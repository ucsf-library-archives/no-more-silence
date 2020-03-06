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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*
upload to box and preserve folder structure
 */
void uploadAndCreateFolder(File fileToUpload, String folderId, BoxAPIConnection client) {

    if (fileToUpload.isFile()) {

        uploadOnefile(fileToUpload, folderId, client)
    }
    else {
        //check if folder exists, if not upload
        //create directory
        BoxFolder folder = new BoxFolder(client, folderId)
        Iterable<BoxItem.Info> children = folder.getChildren()
        boolean folderExists = false
        BoxFolder.Info subFolderInfo
        for (BoxItem.Info child: children) {

            if (child.getName() == fileToUpload.getName()) {

                folderExists = true
                subFolderInfo = child
                println("Found folder ${fileToUpload.getName()}, will not create")
            }
        }

        if (!folderExists) {

            subFolderInfo = folder.createFolder(fileToUpload.getName())
            println("Creat folder ${fileToUpload.getName()}")
        }

        fileToUpload.eachFile() { file ->

            //check if file exists, if not upload
            if (file.isFile()) {

                uploadOnefile(file, subFolderInfo.getID(), client)
            }
            else if (file.isDirectory()) {

                uploadAndCreateFolder(file, subFolderInfo.getID(), client)
            }
            else {

                println(" Do not recognize file type: ${file.getAbsolutePath()}")
            }
        }
    }
}

/*
 * recurse through all folders under filetoUpload and upload to a single folder on box
 */
void uploadAndFlatten(File fileToUpload, String folderId, BoxAPIConnection client) {

    if (fileToUpload.isFile()) {

        uploadOnefile(fileToUpload, folderId, client)
    }
    else {


        fileToUpload.eachFile() { file ->

            //check if file exists, if not upload
            if (file.isFile()) {

                uploadOnefile(file, folderId, client)
            }
            else if (file.isDirectory()) {

                uploadAndFlatten(file, folderId, client)
            }
            else {

                println(" Do not recognize file type: ${file.getAbsolutePath()}")
            }
        }
    }

}
/*
Upload one file
If it exists, it will not uplaod
 */
void uploadOnefile(File fileToUpload, String folderId, BoxAPIConnection client) {

    String fileName = fileToUpload.getName()

    // Select Box folder
    BoxFolder folder = new BoxFolder(client, folderId);

    Iterable<BoxItem.Info> children = folder.getChildren()
    boolean fileExists = false;
    for (BoxItem.Info child: children) {

        if (child.getName() == fileToUpload.getName()) {

            fileExists = true
            println("Found file ${fileToUpload.getName()}, will not create")
        }
    }

    // Upload file
    if (!fileExists) {

        FileInputStream stream = new FileInputStream(fileToUpload.getAbsolutePath());
        BoxFile.Info newFileInfo = folder.uploadFile(stream, fileName);
        stream.close();
        println("Uploading file ${fileToUpload.getAbsolutePath()}")
    }
}

def cli = new CliBuilder(usage: 'CheckArtifacts.groovy')

cli.with {

    h longOpt: 'help', 'Show usage information'
    k longOpt: 'devToken', required: true, args: 1, argName: 'devToken', 'Devtoken issued by the BOX developer console or auth json file'
    f longOpt: 'filePath', required: true, args: 1, argName: 'filePath', 'File to upload'
    t longOpt: 'tagetFolderId', required: true, args: 1, argName: 'targetFolderId', 'Box folder ID to upload to'
    o longOpt: 'uploadOption', required: true, args: 1, argName: 'folder or flat', 'Folder means to preserve folder structure, flat means to upload all files into a single directory'
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
}
else {

    cli.usage()
    return
}

boolean flat = true
if (options.o) {

    if (options.o.equalsIgnoreCase("flat")) {

        flat = true
    }
    else if (options.o.equalsIgnoreCase("folder")) {

        flat = false
    }
    else {

        println("Do not recognize option: ${options.o}")
        cli.usage()
        return
    }

}
else {

    cli.usage()
    return
}

// hack for JCE Unlimited Strength
Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
field.setAccessible(true);

Field modifiersField = Field.class.getDeclaredField("modifiers");
modifiersField.setAccessible(true);
modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

field.set(null, false);
BoxAPIConnection client

if (devToken.toLowerCase().endsWith("json")) { //using auth file

    Reader reader = new FileReader(devToken);
    BoxConfig config = BoxConfig.readFrom(reader);

    client = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(config);

}
else {

    // Create new basic client with developer token as first argument
    client = new BoxAPIConnection(devToken);

}



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

if (flat) {

    uploadAndFlatten(fileToUpload, folderId, client)
}
else {

    uploadAndCreateFolder(fileToUpload, folderId, client)
}
