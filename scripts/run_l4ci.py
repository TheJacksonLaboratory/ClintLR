import argparse, os, sys, glob


# this is the relative location of the jar file that gets built by mvn package
DEFAULT_L4CI_JAR='../l2ci-cli/target/l2ci-cli-0.0.1.jar'

def parseArgs():
	parser = argparse.ArgumentParser(description="LIRICAL analysis of phenopackets (with or without VCF) using a gene list")
	parser.add_argument("jar", default=DEFAULT_L4CI_JAR, help="Path to Java executable JAR file.")
	parser.add_argument("-d", "--data", required=True, help="Path to LIRICAL data directory.")
	parser.add_argument("-e", "--exomiser", help="Path to Exomiser variant database.")
	parser.add_argument("-b", "--background", help="Path to non-default background frequency file.")
	parser.add_argument("-g", "--global", action="store_true", help="Global analysis (default: False).")
	parser.add_argument("--ddndv", action="store_false", help="Disregard a disease if no deleterious variants are found in the gene associated with the disease. "
                        + "Used only if running with a VCF file. (default: True)")
	parser.add_argument("--transcript-db", choices=["REFSEQ", "UCSC"], help="Transcript database (default: REFSEQ)")
	parser.add_argument("--use-orphanet", action="store_true", help="Use Orphanet annotation data (default: False)")
	parser.add_argument("--strict", action="store_true", help="Use strict penalties if the genotype does not match the disease model in terms "
                        + "of number of called pathogenic alleles. (default: False).")
	parser.add_argument("--variant-background-frequency", default=0.1, help="Default background frequency of variants in a gene (default: 0.1).")
	parser.add_argument("--pathogenicity-threshold", default=0.8, help="Variant with greater pathogenicity score is considered deleterious (default: 0.8).")
	parser.add_argument("--default-allele-frequency", default=1e-5, help="Variant with greater allele frequency in at least one population is considered common (default: 1e-5).")
	parser.add_argument("-p", "--phenopacket", nargs="+", help="Path(s) to phenopacket JSON file(s).")
	parser.add_argument("-B", "--batchDir", help="Path to directory containing phenopackets.")
	parser.add_argument("-M", "--mondo", required=True, help="Path to Mondo Ontology JSON file.")
	parser.add_argument("-H", "--hpo", help="Path to HPO Ontology JSON file.")
	parser.add_argument("-A", "--hpoa", help="Path to phenotype.hpoa annotation JSON file.")
	parser.add_argument("-m", "--multiplier", nargs="+", help="Comma-separated pretest adjustment values.")
	parser.add_argument("--vcf", help="Path to VCF variant file.")
	parser.add_argument("-r", "--range", help="File containing ranges of terms for each phenopacket.")
	parser.add_argument("-O", "--outputDirectory", required=True, help="Path to directory to write the results files.")
	parser.add_argument("-o", "--outputFilename", help="Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
	parser.add_argument("--phenotypeOnly", action="store_true", help="Run the analysis with phenotypes only (default: False)") 
	parser.add_argument("--assembly", choices=["hg19", "hg38"], help="Genome build (default: hg38)")
	parser.add_argument("--format", choices=["tsv", "html", "json"], help="Output format (default: tsv)")
	parser.add_argument("--compress", action="store_true", help="Whether to save the output file as a compressed file (default: False)")
	parser.add_argument("-G", "--genes", help="Path to file containing a comma-separated list of gene symbols.") 
	args = parser.parse_args()
	# print(args)

#args = parseArgs()
#l4ci_jar = args.jar
#genes_file = args.genes
##phenopacket = args.phenopacket
#outdir = args.outputDirectory
#outfile = args.outputFilename



def run_l4ci_and_extract_rank(input_phenopacket, correct_diagnosis):
    # todo - make tempfile
    homeDir = os.path.expanduser("~")
    outfile_path = os.path.join(homeDir, "test")
    outfile_name = os.path.join(outfile_path, "test_results")
    l4ci_jar=os.path.abspath('./l2ci-cli/target/l2ci-cli-0.0.1.jar')
    genes_file='scripts/bbs_genes.txt'
    mondo_path = os.path.abspath('./data/mondo.json')
    data = os.path.abspath("./data")
    exomiser = os.path.join(homeDir, "Exomiser/2109_hg19/2109_hg19/2109_hg19_variants.mv.db")
    assembly = "hg19"
    multiplier = "0,5,10"
    arg_list = ["java", "-jar", l4ci_jar, "genes", "--mondo", mondo_path, "-d", data, "-e", exomiser, "--assembly", assembly,
    "-O", outfile_path, "--genes", genes_file, "-B", input_phenopacket, "--phenotype-only", "-m", multiplier]
    command = " ".join(arg_list)
    print(command)
    retval = os.system(command)
    print(f"cmd returned {retval}")

    inpath = ".".join([outfile_name, "tsv"])
    with open(inpath, 'wt') as f:
	    f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "geneFile"]))
	    for file in sorted(glob.glob("*.tsv")):
	        if not file == outfile_name:
	            with open(file) as pf:
	                fileName = os.path.basename(pf.name)
	                phenopacketName = fileName.split("target")[0][:-1] if "target" in fileName else fileName.split("genes")[0][:-1]
	                pretestAdjustment = fileName.split("multiplier")[1][1:-4]
	                targetOmim = [correct_diagnosis[key] for key in correct_diagnosis.keys() if key in phenopacketName]
	                targetLine = [line for line in pf if targetOmim[0] in line]
	                targetItems = targetLine[0].split("\t")
	                rank = targetItems[0]
	                diseaseId = targetItems[2]
	                diseaseLabel = targetItems[1]
	                pretestProb = targetItems[3]
	                posttestProb = targetItems[4]
	                genesFile = genes_file if "genes" in file else "N/A"
	                f.write("\n")
	                f.write("\t".join([phenopacketName, diseaseId, diseaseLabel, rank, pretestAdjustment, pretestProb, posttestProb, genesFile]))
    print("Wrote results to: " + inpath)


#homeDir = os.path.expanduser("~")
#ppak = os.path.join(homeDir, "phenopackets/Bardet_Biedel_Syndrome/")
ppak = 'scripts/test_data'
# right_dx = "OMIM:209900" #"MONDO:0008842"
right_dx = {"Ajmal": "OMIM:209900", "Bee": "OMIM:615981", "Imani": "OMIM:615983", "Li": "OMIM:615982"}

run_l4ci_and_extract_rank(input_phenopacket=ppak, correct_diagnosis=right_dx)

  