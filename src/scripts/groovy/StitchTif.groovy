/**
 * Created by Rebecca Tang on Apr/10/2020.
 *
 * Quick script to stitch together tif images for LSTA file because tifs are downloaded as single page images
 */

import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageCodec;

def cl = new CliBuilder(usage: 'groovy ExtractTextFromPDF.groovy -i inputDir -o optionalOutputDir')
cl.i(argName: 'inputdir', longOpt: 'inputdir', args:1, required: true, 'Input Directory')
cl.o(argName: 'outputdir', longOpt: 'outputdir', args:1, 'Output Directory, if empty, then output to input dir')
cl.v(argName: 'verbose', longOpt: 'verbose', 'Show verbose messages')
cl.h(longOpt:'help', 'Show usage information and quit')


def opt = cl.parse(args)
if (!opt) {

    System.exit(1)
}
File inputDir  = new File(opt["i"])

println("${opt["o"]}")
File outputDir = opt["o"]?(new File(opt["o"])):inputDir
if (!inputDir.exists()) {

    println("ERROR: $inputDir does not exists")
    System.exit(1)

}
if (!outputDir.exists()) {

    println("ERROR: $outputDir does not exists")
    System.exit(1)
}
verbose = opt["v"]?true:false

println("Processing inputDir=$inputDir and outptuDir=$outputDir")

if (verbose) {

    println("Verbose mode on")
}

//Create a map of key = root, value = list of file names
Map<String,List> fileMap = [:]

def dirFiles = 0
inputDir.eachFileMatch( ~/.*tif/){ tifFile ->

    dirFiles++
    String fileName = tifFile.getName()
    def groupName = (fileName =~ /(?:(?!(_)(?!.*_)).)*/) //search until_, but not including the last _
    if (groupName.hasGroup() && groupName.size()!=0) {

        //println(groupName[0][0]) //debug

        List fileList = fileMap.get(groupName[0][0], [])
        fileList.add(fileName)

    } else {

        println "WARNING: Could not find a pattern for ${tifFile.getName()}"
    }
}
println(fileMap.size())

fileMap.each{ itemName, fileList ->

    //println "Processing files for items named $itemName"
    if (fileList.size() > 1) {

        //postfixes.sort{a,b ->
        //
        //    def  ga = (  a =~ /Series.*Item\d{1,4}-(?:Insert-)?(\d{1,4}).*/ )
        //    def  gb = (  b =~ /Series.*Item\d{1,4}-(?:Insert-)?(\d{1,4}).*/ )
        //
        //    def na = ga[0][1] as int
        //    def nb=  gb[0][1] as int
        //    na <=> nb
        //}


        StringBuilder sb = new StringBuilder("tiffcp ")
        fileList.each { file ->

            sb.append(inputDir).append("/").append(file).append(" ")

        }

        sb.append(outputDir).append("/").append(itemName).append(".tif")

        println("=====>")
        println(sb) //debug
  //      def sout = new StringBuilder()
   //     def serr = new StringBuilder()

     //   def proc = sb.toString().execute()
     //   proc.consumeProcessOutput(sout, serr)

     //   proc.waitFor()

    } else {

        // there is just one page, so just move it to the output dir
        StringBuilder sb = new StringBuilder("cp ")
        fileList.each { file ->

            sb.append(inputDir).append("/").append(file).append(" ")

        }
        sb.append(outputDir).append("/.")

        println("=====>") //debug
        println(sb) //debug
      //  def sout = new StringBuilder()
      //  def serr = new StringBuilder()

     //   def proc = sb.toString().execute()
     //   proc.consumeProcessOutput(sout, serr)

      //  proc.waitFor()
    }
}


// reconcile pages
def totalPagesFromSingles = 0
fileMap.each { itemName, fileList ->

    def count = fileList.size()
    println("$itemName -> ${fileList.size()}")
    totalPagesFromSingles += count

}
println("INFO: Total pages from singles $totalPagesFromSingles")

def totalPagesFromCombined = 0
outputDir.eachFile { file ->

    int pages = pages(file)
    totalPagesFromCombined += pages
    println("${file.getName()} -> ${pages}")
}
println("INFO: Total pages from combined $totalPagesFromCombined")

int pages(file){

    SeekableStream s = new FileSeekableStream(file);
    TIFFDecodeParam param = null;
    ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
    return dec.getNumPages()
}