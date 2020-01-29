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
 * Created by Rebecca Tang on 11/7/18.
 *
 * Created for the UCSF Archives LSTA project
 * Extracts Text from PDF file
 * inputDir is where the pdf files are
 * outputDir is where you want the .ocr files to be written
 * If if you want .ocr files to be written in the same directory as the pdf files, then just omit -o parameter
 *
 * subdirectories will be handled.
 *
 *
 * ex: groovy ExtractTextFromPDF.groovy -i /User/XXXX/downloadedPdfs -v
 */


@Grab(group='com.itextpdf', module='kernel', version='7.1.3')
@Grab(group='com.itextpdf', module='layout', version='7.1.3')


import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import groovy.io.FileType

def cl = new CliBuilder(usage: 'groovy ExtractTextFromPDF.groovy -i inputDir -o optionalOutputDir')
cl.i(argName: 'inputdir', longOpt: 'inputdir', args:1, required: true, 'Input Directory')
cl.o(argName: 'outputdir', longOpt: 'outputdir', 'Output Directory, if empty, then output to input dir')
cl.v(argName: 'verbose', longOpt: 'verbose', 'Show verbose messages')
cl.h(longOpt:'help', 'Show usage information and quit')


def opt = cl.parse(args)
if (!opt) {

	System.exit(1)
}
File inputDir  = new File(opt["i"])


File outputDir = opt["o"]?(new File(opt["o"])):inputDir
verbose = opt["v"]?true:false

println("Processing inputDir=$inputDir and outptuDir=$outputDir")

if (verbose) {

	println("Verbose mode on")
}

def fileCounter = 0
inputDir.eachFileRecurse (FileType.FILES) { file ->

	if (file.getName().endsWith(".pdf")) {

		File outSubDir = new File(outputDir, file.getPath().substring(inputDir.getPath().length(), file.getPath().length()-file.getName().length()-1))
		println("outSubDir $outSubDir")

		if (!outSubDir.exists()) {

			outSubDir.mkdirs()
			if (verbose) println("Creating dir $outSubDir")
		}
		File ocrFile = new File(outSubDir, "${file.getName().substring(0,file.getName().length()-4)}.ocr")
		PrintWriter writer = new PrintWriter(ocrFile, "UTF-8");
		PdfReader reader = null;
		PdfDocument pdfDoc = null;
		try {

			if (verbose) println("Creating text file ${ocrFile.getName()} from ${file.getName()}")
			reader = new PdfReader(file)
			pdfDoc = new PdfDocument(reader)

			for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {

				String str = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), new LocationTextExtractionStrategy());
				writer.println(str);
				writer.println("pgNbr="+i);
				if (verbose) println("Processing page $i")
			}
		} catch (Exception e) {

			throw(e)
		} finally {

			if (writer!=null) {

				writer.close()
			}
			if (pdfDoc!= null) {

				pdfDoc.close()
			}
			if(reader != null){

				reader.close()
			}
		}
		fileCounter++
		if (verbose) println("Processed $fileCounter files")
	}

}

if (verbose) println("Total files extracted $fileCounter")

if (verbose) println("Done")

System.exit(0)