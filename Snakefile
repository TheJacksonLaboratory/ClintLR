import re, gzip, os, glob
from itertools import product
# from google.protobuf.json_format import MessageToJson, Parse

shell.prefix("""
# http://linuxcommand.org/wss0150.php
PROGNAME=$(basename $0)
function error_exit
{{
#   ----------------------------------------------------------------
#   Function for exit due to fatal program error
#       Accepts 1 argument:
#           string containing descriptive error message
#   ----------------------------------------------------------------
    echo "${{PROGNAME}}: ${{1:-"Unknown Error"}}" 1>&2
    exit 1
}}
""")



resources_dir='resources'

homeDir = os.path.expanduser("~")
exomiser = os.path.join(homeDir, "Exomiser/2109_hg19/2109_hg19/2109_hg19_variants.mv.db")

# this is the location of the jar file that gets built by mvn package
parentPath = os.getcwd() #os.path.abspath(os.path.dirname(scriptParent))
DEFAULT_L4CI_JAR=os.path.join(parentPath, 'l4ci-cli', 'target', 'L4CI-CLI.jar')
DEFAULT_DATA_DIR=os.path.join(parentPath, 'data')
DEFAULT_VCF_FILE=os.path.join(parentPath, 'scripts', 'project.NIST.hc.snps.indels.NIST7035.vcf')
DEFAULT_CIRANGES_FILE=os.path.join(parentPath, 'scripts', 'DiseaseIntuitionGroups.tsv')
DEFAULT_PRETEST_ADJUSTMENT="0"

def extract_correct_diagnosis_from_phenopacket(phenopacket_file):
    if not os.path.isfile(phenopacket_file):
        raise FileNotFoundError(f"Could not find file at {phenopacket_file}")
    diagnosis_curie = None
    with open(phenopacket_file) as f:
        data = f.read()
        jsondata = json.loads(data)
#             phenopacket = Parse(json.dumps(jsondata), Phenopacket())
        if len(jsondata['diseases']) == 1:
            diseaseId = jsondata['diseases'][0]['term']['id']
            diagnosis_curie = diseaseId
        elif len(phenopacket.diseases) > 1:
            raise ValueError(f"Error: phenopacket has two different diagnoses!")
    if diagnosis_curie is None:
       raise ValueError(f"Could not extract diagnosis from {phenopacket_file}")
    return diagnosis_curie

def extract_CIrange_selected_term(CIranges_file, phenopacket_name, CIrange):
    CIrangeTerm, CIrangeLabel = "", ""
    with open(CIranges_file, 'r') as cf:
        extractPhenopacketLine = [line for line in cf if phenopacket_name in line]
        if len(extractPhenopacketLine) > 0:
            phenopacketLine = extractPhenopacketLine[0]
            rangeItems = phenopacketLine.split("\t")
            mondoTerms = [item for item in rangeItems if "MONDO" in item]
            if CIrange == "target":
                if len(mondoTerms) > 0:
                    CIrangeTerm = mondoTerms[0]
                    CIrangeLabel = rangeItems[rangeItems.index(CIrangeTerm) + 2]
            elif CIrange == "narrow":
                if len(mondoTerms) > 1:
                    CIrangeTerm = mondoTerms[1]
                    CIrangeLabel = rangeItems[rangeItems.index(CIrangeTerm) + 1]
            elif CIrange == "broad":
                if len(mondoTerms) > 2:
                    CIrangeTerm = mondoTerms[2]
                    CIrangeLabel = rangeItems[rangeItems.index(CIrangeTerm) + 1]

    return CIrangeTerm, CIrangeLabel

def extract_rank(output_file, output_file_name, phenopacket_analysis_file, phenopacket_name, correct_diagnosis):
    if not phenopacket_analysis_file == output_file_name and phenopacket_name in phenopacket_analysis_file:
        with gzip.open(phenopacket_analysis_file, 'rt') as rf:
            fileName = os.path.basename(rf.name)
            m = re.search('multiplier_(.+?).tsv', fileName)
            pretestAdjustment = m.group(1) if m else "N/A"
            targetOmim = correct_diagnosis #[correct_diagnosis[key] for key in correct_diagnosis.keys() if key in phenopacketName][0]
            extractTargetLine = [line for line in rf if targetOmim in line]
            if len(extractTargetLine) > 0:
                targetLine = extractTargetLine[0]
                targetItems = targetLine.split("\t")
                rank = targetItems[0]
                diseaseId = targetItems[2]
                diseaseLabel = targetItems[1]
                pretestProb = targetItems[3]
                posttestProb = targetItems[4]
                CIrange, CIrangeTerm, CIrangeLabel = "", "", ""
                if "target" in phenopacket_analysis_file:
                    CIrange = "target"
                    CIrangeTerm, CIrangeLabel = extract_CIrange_selected_term(DEFAULT_CIRANGES_FILE, phenopacket_name, CIrange)
                elif "narrow" in phenopacket_analysis_file:
                    CIrange = "narrow"
                    CIrangeTerm, CIrangeLabel = extract_CIrange_selected_term(DEFAULT_CIRANGES_FILE, phenopacket_name, CIrange)
                elif "broad" in phenopacket_analysis_file:
                    CIrange = "broad"
                    CIrangeTerm, CIrangeLabel = extract_CIrange_selected_term(DEFAULT_CIRANGES_FILE, phenopacket_name, CIrange)
                output_file.write("\n")
                output_file.write("\t".join([phenopacket_name, diseaseId, diseaseLabel, rank, pretestAdjustment, pretestProb, posttestProb, CIrange, CIrangeTerm, CIrangeLabel]))



phenopacketNames = [os.path.splitext(os.path.basename(file))[0] for file in sorted(glob.glob(os.path.join(os.path.abspath("resources/phenopackets_v2/"), "*.json")))]
multiplierValues = ["0.0", "1.0", "5.0", "10.0", "15.0", "20.0"]
types = ["phenotype"]


rule all:
  input:
#     expand(os.path.abspath("resources/batchOutput/{type}/{phenopacketName}_target_multiplier_{multiplierValue}.tsv.gz"),
#         phenopacketName=phenopacketNames,
#         multiplierValue=multiplierValues,
#         type=types),
    expand(os.path.abspath("resources/batchOutput/{type}/global/{phenopacketName}_target_multiplier_0.0.tsv.gz"),
            phenopacketName=phenopacketNames,
            type=types),
#     "resources/phenotype/l4ci_batch_analysis_results.tsv",
    "resources/phenotype/l4ci_batch_analysis_results_uniform.tsv"


rule runL4CIBatchAnalysisGenotype:
  input:
    os.path.abspath("resources/phenopackets_v2/{phenopacketName}.json")
  output:
    os.path.abspath("resources/batchOutput/genotype/{phenopacketName}_target_multiplier_{multiplierValue}.tsv.gz")
  params:
    cluster_opts='--mem-per-cpu=36G -t 48:00:00',
    resources_dir = resources_dir,
    exomiser = exomiser,
    data_dir = DEFAULT_DATA_DIR,
    l4ci_jar = DEFAULT_L4CI_JAR,
    mondo_path = os.path.join(DEFAULT_DATA_DIR, "mondo.json"),
    vcf_file = os.path.abspath("scripts/project.NIST.hc.snps.indels.NIST7035.vcf"),
    CIranges_file = os.path.abspath("scripts/DiseaseIntuitionGroups.tsv"),
    output_directory = os.path.abspath("resources/batchOutput/genotype")
  log:
    "resources/genotype/{phenopacketName}_multiplier_{multiplierValue}_Log.log"
  shell:
    """
    (mkdir -p {params.resources_dir}
    cd {params.resources_dir}
    java -jar {params.l4ci_jar} batch -M {params.mondo_path} -d {params.data_dir} -e {params.exomiser} -p {input} --assembly "hg19" --vcf {params.vcf_file} -r {params.CIranges_file} -m {wildcards.multiplierValue} --compress -O {params.output_directory}) &> {log}
    """

rule runL4CIBatchAnalysisGenotypeUniform:
  input:
    os.path.abspath("resources/phenopackets_v2/{phenopacketName}.json")
  output:
    os.path.abspath("resources/batchOutput/genotype/global/{phenopacketName}_target_multiplier_0.0.tsv.gz")
  params:
    cluster_opts='--mem-per-cpu=36G -t 48:00:00',
    resources_dir = resources_dir,
    exomiser = exomiser,
    data_dir = DEFAULT_DATA_DIR,
    l4ci_jar = DEFAULT_L4CI_JAR,
    mondo_path = os.path.join(DEFAULT_DATA_DIR, "mondo.json"),
    vcf_file = os.path.abspath("scripts/project.NIST.hc.snps.indels.NIST7035.vcf"),
    CIranges_file = os.path.abspath("scripts/DiseaseIntuitionGroups.tsv"),
    output_directory = os.path.abspath("resources/batchOutput/genotype/global")
  log:
    "resources/genotype/global/{phenopacketName}_uniform_Log.log"
  shell:
    """
    (mkdir -p {params.resources_dir}
    cd {params.resources_dir}
    java -jar {params.l4ci_jar} batch -M {params.mondo_path} -d {params.data_dir} -e {params.exomiser} -p {input} --assembly "hg19" --vcf {params.vcf_file} -r {params.CIranges_file} -g --compress -O {params.output_directory}) &> {log}
    """

rule runL4CIBatchAnalysisPhenotype:
  input:
    os.path.abspath("resources/phenopackets_v2/{phenopacketName}.json")
  output:
    os.path.abspath("resources/batchOutput/phenotype/{phenopacketName}_target_multiplier_{multiplierValue}.tsv.gz")
  params:
    cluster_opts='--mem-per-cpu=36G -t 48:00:00',
    resources_dir = resources_dir,
    exomiser = exomiser,
    data_dir = DEFAULT_DATA_DIR,
    l4ci_jar = DEFAULT_L4CI_JAR,
    mondo_path = os.path.join(DEFAULT_DATA_DIR, "mondo.json"),
    vcf_file = os.path.abspath("scripts/project.NIST.hc.snps.indels.NIST7035.vcf"),
    CIranges_file = os.path.abspath("scripts/DiseaseIntuitionGroupsTsv.tsv"),
    output_directory = os.path.abspath("resources/batchOutput/phenotype")
  log:
    "resources/phenotype/{phenopacketName}_multiplier_{multiplierValue}_Log.log"
  shell:
    """
    (mkdir -p {params.resources_dir}
    cd {params.resources_dir}
    java -jar {params.l4ci_jar} batch -M {params.mondo_path} -d {params.data_dir} -e {params.exomiser} -p {input} --assembly "hg19" -r {params.CIranges_file} -m {wildcards.multiplierValue} --compress -O {params.output_directory}) &> {log}
    """

rule runL4CIBatchAnalysisPhenotypeUniform:
  input:
    os.path.abspath("resources/phenopackets_v2/{phenopacketName}.json")
  output:
    os.path.abspath("resources/batchOutput/phenotype/global/{phenopacketName}_target_multiplier_{multiplierValue}.tsv.gz")
  params:
    cluster_opts='--mem-per-cpu=36G -t 48:00:00',
    resources_dir = resources_dir,
    exomiser = exomiser,
    data_dir = DEFAULT_DATA_DIR,
    l4ci_jar = DEFAULT_L4CI_JAR,
    mondo_path = os.path.join(DEFAULT_DATA_DIR, "mondo.json"),
    vcf_file = os.path.abspath("scripts/project.NIST.hc.snps.indels.NIST7035.vcf"),
    CIranges_file = os.path.abspath("scripts/DiseaseIntuitionGroups.tsv"),
    output_directory = os.path.abspath("resources/batchOutput/phenotype/global")
  log:
    "resources/phenotype/global/{phenopacketName}_multiplier_{multiplierValue}_Log.log"
  shell:
    """
    (mkdir -p {params.resources_dir}
    cd {params.resources_dir}
    java -jar {params.l4ci_jar} batch -M {params.mondo_path} -d {params.data_dir} -e {params.exomiser} -p {input} --assembly "hg19" -r {params.CIranges_file} -g --compress -O {params.output_directory}) &> {log}
    """

rule createOutputFileGenotype:
  input:
    sorted(glob.glob(os.path.join(os.path.abspath("resources/batchOutput/genotype/"), "*.tsv.gz")))
  output:
    os.path.abspath("resources/genotype/l4ci_batch_analysis_results.tsv")
  log:
    os.path.abspath("resources/genotype/l4ci_batch_analysis_results_Log.log")
  run:
    with open(str(output), 'wt') as f:
        print("Writing results file...")
        f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "CIrange", "CIrangeTerm", "CIrangeLabel"]))
        for phenop_analysis_file in sorted(input):
            phenop = "_".join(phenop_analysis_file.split("_")[0:-3]).split("/")[-1]
            phenopFile = "resources/phenopackets_v2/" + phenop + ".json"
            right_dx = extract_correct_diagnosis_from_phenopacket(phenopFile)
            extract_rank(output_file=f, output_file_name=str(output), phenopacket_analysis_file=phenop_analysis_file, phenopacket_name=phenop, correct_diagnosis=right_dx)
        print("Wrote results to: " + str(output))

rule createOutputFileGenotypeUniform:
  input:
    sorted(glob.glob(os.path.join(os.path.abspath("resources/batchOutput/genotype/global/"), "*.tsv.gz")))
  output:
    os.path.abspath("resources/genotype/l4ci_batch_analysis_results_uniform.tsv")
  log:
    os.path.abspath("resources/genotype/l4ci_batch_analysis_results_uniform_Log.log")
  run:
    with open(str(output), 'wt') as f:
        print("Writing results file...")
        f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "CIrange", "CIrangeTerm", "CIrangeLabel"]))
        for phenop_analysis_file in sorted(input):
            phenop = "_".join(phenop_analysis_file.split("_")[0:-3]).split("/")[-1]
            phenopFile = "resources/phenopackets_v2/" + phenop + ".json"
            right_dx = extract_correct_diagnosis_from_phenopacket(phenopFile)
            extract_rank(output_file=f, output_file_name=str(output), phenopacket_analysis_file=phenop_analysis_file, phenopacket_name=phenop, correct_diagnosis=right_dx)
        print("Wrote results to: " + str(output))


rule createOutputFilePhenotype:
  input:
    sorted(glob.glob(os.path.join(os.path.abspath("resources/batchOutput/phenotype/"), "*.tsv.gz")))
  output:
    os.path.abspath("resources/phenotype/l4ci_batch_analysis_results.tsv")
  log:
    os.path.abspath("resources/phenotype/l4ci_batch_analysis_results_Log.log")
  run:
    with open(str(output), 'wt') as f:
        print("Writing results file...")
        f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "CIrange", "CIrangeTerm", "CIrangeLabel"]))
        for phenop_analysis_file in sorted(input):
            phenop = "_".join(phenop_analysis_file.split("_")[0:-3]).split("/")[-1]
            phenopFile = "resources/phenopackets_v2/" + phenop + ".json"
            right_dx = extract_correct_diagnosis_from_phenopacket(phenopFile)
            extract_rank(output_file=f, output_file_name=str(output), phenopacket_analysis_file=phenop_analysis_file, phenopacket_name=phenop, correct_diagnosis=right_dx)
        print("Wrote results to: " + str(output))

rule createOutputFilePhenotypeUniform:
  input:
    sorted(glob.glob(os.path.join(os.path.abspath("resources/batchOutput/phenotype/global/"), "*.tsv.gz")))
  output:
    os.path.abspath("resources/phenotype/l4ci_batch_analysis_results_uniform.tsv")
  log:
    os.path.abspath("resources/phenotype/l4ci_batch_analysis_results_uniform_Log.log")
  run:
    with open(str(output), 'wt') as f:
        print("Writing results file...")
        f.write("\t".join(["phenopacket", "diseaseID", "diseaseLabel", "rank", "pretestAdjustment", "pretestProbability", "posttestProbability", "CIrange", "CIrangeTerm", "CIrangeLabel"]))
        for phenop_analysis_file in sorted(input):
            phenop = "_".join(phenop_analysis_file.split("_")[0:-3]).split("/")[-1]
            phenopFile = "resources/phenopackets_v2/" + phenop + ".json"
            right_dx = extract_correct_diagnosis_from_phenopacket(phenopFile)
            extract_rank(output_file=f, output_file_name=str(output), phenopacket_analysis_file=phenop_analysis_file, phenopacket_name=phenop, correct_diagnosis=right_dx)
        print("Wrote results to: " + str(output))
