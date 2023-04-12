import argparse, os, sys, glob, re
from phenopackets import *
import json
from google.protobuf.json_format import MessageToJson, Parse



# this is the location of the jar file that gets built by mvn package
scriptParent = os.path.dirname(sys.argv[0])
parentPath = os.path.abspath(os.path.dirname(scriptParent))
grandparentPath = os.path.abspath(os.path.dirname(parentPath))
DEFAULT_L4CI_JAR=os.path.join(parentPath, 'l4ci-cli', 'target', 'L4CI.jar')
DEFAULT_DATA_DIR=os.path.join(parentPath, 'data')
DEFAULT_OUT_DIR=os.path.abspath(scriptParent) #"."
DEFAULT_GENES_FILE=os.path.join(parentPath, 'scripts', 'bbs_genes.txt')
DEFAULT_PRETEST_ADJUSTMENT="0"



def extract_correct_diagnosis_from_phenopacket(phenopacket_file):
    if not os.path.isfile(phenopacket_file):
        raise FileNotFoundError(f"Could not find file at {phenopacket_file}")
    diagnosis_curie = None
    with open(phenopacket_file) as f:
        data = f.read()
        jsondata = json.loads(data)
        phenopacket = Parse(json.dumps(jsondata), Phenopacket())
        if len(phenopacket.diseases) == 1:
            diseaseId = phenopacket.diseases[0].term.id
            diagnosis_curie = diseaseId
        elif len(phenopacket.diseases) > 1:
            raise ValueError(f"Error: phenopacket has two different diagnoses!")
    if diagnosis_curie is None:
       raise ValueError(f"Could not extract diagnosis from {phenopacket_file}")
    return diagnosis_curie
         
         

   
def run_l4ci_and_extract_rank(l4ci_jar, mondo_path, output_directory, input_phenopacket, correct_diagnosis, multiplier, genes_file):
    homeDir = os.path.expanduser("~")
    phenopacketName = os.path.basename(input_phenopacket).rsplit('.', 1)[0]
    exomiser = os.path.join(homeDir, "Exomiser/2109_hg19/2109_hg19/2109_hg19_variants.mv.db")

    arg_list = ["java", "-jar", l4ci_jar, "genes", "-d", data_dir, "--genes", genes_file, "-O", output_directory, "-M", mondo_path,
                "-p", input_phenopacket, "-e", exomiser, "-m", multiplier]
    command = " ".join(arg_list)
    print(command)
    retval = os.system(command)
    print(f"cmd returned {retval}")


    for file in sorted(glob.glob(os.path.join(os.path.dirname(inpath), "*tsv"))):
        if not file == inpath and phenopacketName in file:
            with open(file) as rf:
                fileName = os.path.basename(rf.name)
                m = re.search('multiplier_(.+?).tsv', fileName)
                pretestAdjustment = m.group(1) if m else "N/A"
                targetOmim = correct_diagnosis #[correct_diagnosis[key] for key in correct_diagnosis.keys() if key in phenopacketName][0]
                targetLine = [line for line in rf if targetOmim in line][0]
                targetItems = targetLine.split("\t")
                rank = targetItems[0]
                diseaseId = targetItems[2]
                diseaseLabel = targetItems[1]
                pretestProb = targetItems[3]
                posttestProb = targetItems[4]
                genesFile = os.path.basename(genes_file) if "genes" in file else "N/A"
                f.write("\n")
                f.write("\t".join([phenopacketName, diseaseId, diseaseLabel, rank, pretestAdjustment, pretestProb, posttestProb, genesFile]))

  
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="LIRICAL analysis of phenopackets (with or without VCF) using a gene list")
    parser.add_argument("-j", "--jar", nargs='?', default=DEFAULT_L4CI_JAR, help="Path to Java executable JAR file.")
    parser.add_argument("-d", "--data", nargs='?', default=DEFAULT_DATA_DIR, help="Path to LIRICAL data directory.")
    parser.add_argument("-O", "--outputDirectory", nargs="?", default=DEFAULT_OUT_DIR, help="Path to directory to write the results files.")
    parser.add_argument("-m", "--multiplier", default=DEFAULT_PRETEST_ADJUSTMENT, help="Comma-separated pretest adjustment values.")
    parser.add_argument("-G", "--genes", default=DEFAULT_GENES_FILE, help="Path to file containing a comma-separated list of gene symbols.")
    parser.add_argument("-p", "--phenopacket", required=True, help="Path(s) to phenopacket JSON file(s).")
    args = parser.parse_args()
    l4ci_jar = args.jar
    if not os.path.isfile(l4ci_jar):
        print(f"Tried and failed to find the L4CI jar file at {l4ci_jar}")
        raise ValueError("Could not find L4CI executable JAR file. Either build the L4CI package using maven for the default location or set path with -j/--jar")
    data_dir = args.data
    if not os.path.isdir(data_dir):
        print(f"Tried and failed to find data directory at {data_dir}")
        raise ValueError("Could not find data dir. Consider running lc4i-cli download command or set path with -d/--data")
    mondoPath = os.path.join(data_dir, "mondo.json")
    outdir = args.outputDirectory
    phenop = args.phenopacket
    mult = args.multiplier
    genes = args.genes

    outfile_name = os.path.join(outdir, "l4ci_gene_analysis_results")


    if os.path.isfile(phenop):
        right_dx = extract_correct_diagnosis_from_phenopacket(phenop)
        inpath = ".".join([outfile_name, "tsv"])
        with open(inpath, 'wt') as f:
            f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "geneFile"]))
            run_l4ci_and_extract_rank(l4ci_jar=l4ci_jar, 
                                    mondo_path=mondoPath, 
                                    output_directory=outdir, 
                                    input_phenopacket=phenop, 
                                    correct_diagnosis=right_dx, 
                                    multiplier=mult, 
                                    genes_file=genes)
            print("Wrote results to: " + inpath)

    elif os.path.isdir(phenop):
        inpath = ".".join([outfile_name, "tsv"])
        with open(inpath, 'at') as f:
            f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "geneFile"]))
            for phenopFile in sorted(glob.glob(os.path.join(phenop, "*json"))):
                right_dx = extract_correct_diagnosis_from_phenopacket(phenopFile)
                run_l4ci_and_extract_rank(l4ci_jar=l4ci_jar,
                                        mondo_path=mondoPath, 
                                        output_directory=outdir, 
                                        input_phenopacket=phenopFile, 
                                        correct_diagnosis=right_dx, 
                                        multiplier=mult, 
                                        genes_file=genes)
                print("Wrote results to: " + inpath)
    
