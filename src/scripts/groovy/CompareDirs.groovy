
def cl = new CliBuilder(usage: 'groovy CompareDirs.groovy -i inputDir -c compareDirs')
cl.i(argName: 'inputdir', longOpt: 'inputdir', args:1, required: true, 'Input Directory')
cl.c(argName: 'compare', longOpt: 'compdir', args:1, required: false, 'The directory to compare to the input dir')
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

List<String> inputFileList = new ArrayList<String>()

inputDir.eachFile { file ->

    inputFileList.add(file.getName())
}

int intersectionCount = 0;
compDir.eachFile { file->

    if (inputFileList.contains(file.getName())) {

        intersectionCount++;
        println(file.getName())
    }
}
println("total intersection: $intersectionCount")
