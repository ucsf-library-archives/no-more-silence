/**
 * Created by Rebecca Tang on Apr/10/2020.
 *
 * Quick script to print information on directory and files in the directory.
 * Used to reconcile files
 */

import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageCodec;

def cl = new CliBuilder(usage: 'groovy ExtractTextFromPDF.groovy -i inputDir -c compareDirs')
cl.i(argName: 'inputdir', longOpt: 'inputdir', args:1, required: true, 'Input Directory')
cl.c(argName: 'compare', longOpt: 'compdir', args:1, required: false, 'The directory to compare to the input dir')
cl.t(argname: 'compareType', longOpt: 'compType', args:1, required: false, 'intersection or difference')
cl.v(argName: 'verbose', longOpt: 'verbose', 'Show verbose messages')
cl.h(longOpt:'help', 'Show usage information and quit')


def opt = cl.parse(args)
if (!opt) {

    System.exit(1)
}

File inputDir = new File(opt["i"])

if (!inputDir.exists()) {

    println("ERROR: $inputDir does not exists")
    System.exit(1)
}

File compDir
if (opt.c) {

    compDir = new File(opt["c"])
    if (!compDir.exists()) {

        println("ERROR: the comparison dir does not exist")
        System.exit(1)
    }
}
verbose = opt["v"] ? true : false

println("Processing inputDir=$inputDir")


if (verbose) {

    println("Verbose mode on")
}

//key = file type, value = count
Map<String,Integer> fileMap = [:]

def dirFiles = 0
inputDir.eachFile{ file ->

    dirFiles++
    String fileName = file.getName()
    def fileType = (fileName =~ /(\.[^.]+)$/ )  //(fileName =~ /[^.]*$/ ) //find suffix
    //println(fileType)
    if (fileType.hasGroup() && fileType.size()!=0) {

      //  println(fileType[0][0]) //debug

        int count = fileMap.get(fileType[0][0], 0)
        count++
        fileMap.put(fileType[0][0], count)

    } else {

        println "WARNING: Could not find a pattern for ${file.getName()}"
    }
}
println("Total number of files $dirFiles")
println(fileMap)

if (compDir) {

    Map<String,Integer> compDirFileMap = [:]
    def compDirFiles = 0

    compDir.eachFile{ file ->

        compDirFiles++
        String fileName = file.getName()
        def fileType = (fileName =~ /(\.[^.]+)$/ )  //(fileName =~ /[^.]*$/ ) //find suffix
        //println(fileType)
        if (fileType.hasGroup() && fileType.size()!=0) {

            //  println(fileType[0][0]) //debug

            int count = compDirFileMap.get(fileType[0][0], 0)
            count++
            compDirFileMap.put(fileType[0][0], count)

        } else {

            println "WARNING: Could not find a pattern for ${file.getName()}"
        }
    }
    println("Total number of files $compDirFiles")
    println(compDirFileMap)

    inputDir.eachFileMatch( ~/.*tif/) { dir1File ->

        String fileName = dir1File.getName()
        def fileCleanName = (fileName =~ /(?:(?!(\.)(?!.*\.)).)*/ )  //file name with no suffix
        if (fileCleanName.hasGroup() && fileCleanName.size()!=0) {

            //look for .ocr in dir2
            //println(fileCleanName[0][0])
            File ocrFile = new File(compDir, fileCleanName[0][0] + ".ocr")
            if (!ocrFile.exists()) {

                println("ocr file does not exist for ${dir1File.getName()}")
            }

        } else {

            println("WARNING: Could not find a pattern for ${dir1File.getName()}")
        }
    }

    compDir.eachFileMatch( ~/.*ocr/) { compDirFile ->

        String fileName = compDirFile.getName()
        def fileCleanName = (fileName =~ /(?:(?!(\.)(?!.*\.)).)*/ )  //file name with no suffix
        if (fileCleanName.hasGroup() && fileCleanName.size()!=0) {

            //look for .ocr in dir2
            //println(fileCleanName[0][0])
            File ocrFile = new File(inputDir, fileCleanName[0][0] + ".tif")
            if (!ocrFile.exists()) {

                println("tif file does not exist for ${compDirFile.getName()}")
            }

        } else {

            println("WARNING: Could not find a pattern for ${compDirFile.getName()}")
        }
    }

}
