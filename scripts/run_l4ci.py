import argparse, os, sys


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
	outfile_name = input_phenopacket + "-results.tsv"
	l4ci_jar=os.path.abspath('../l2ci-cli/target/l2ci-cli-0.0.1.jar')
	genes_file='scripts/bbs_genes.txt'
	mondo_path = os.path.abspath('../data/mondo.json')
	data = "/Users/robinp/.l4ci/data/lirical"
	arg_list = ["java", "-jar", l4ci_jar, "genes", "--mondo", mondo_path, "-d", data, "-O", ".", "--genes", genes_file, "--phenopacket", input_phenopacket, "-o", outfile_name, "--phenotype-only"]
	command = " ".join(arg_list)
	print(command)
	retval = os.system(command)
	print(f"cmd returned {retval}")
    
	inpath = outfile_name
	with open(inpath) as f:
		for line in f:
			print(line)



ppak = "/Users/robinp/Downloads/phenopackets/Inlora-2017-APTX-V-3.json"
right_dx = "MONDO:0008842"

run_l4ci_and_extract_rank(input_phenopacket=ppak, correct_diagnosis=right_dx)

  