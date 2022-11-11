# L4CI
LIRICAL for clinical intuition



mvn archetype:generate \
-DarchetypeGroupId=org.monarchinitiative.l2ci \
-Dfilter=pom-root \
-DarchetypeVersion=RELEASE

## Making the mondo.json file

L4CI reads the Mondo ontology as a JSON file. The mondo in OWL format can be converted to a JSON file using obographs by typing the following into the Terminal:

```$ obographs convert mondo.owl -f json```

## Installing the LIRICAL dependency

Clone the LIRICAL project from GitHub, then checkout and package the *modularize* branch.

```
$ git clone https://github.com/TheJacksonLaboratory/LIRICAL.git
$ cd LIRICAL
$ git checkout modularize
$ ./mvnw --projects lirical-cli,lirical-benchmark --also-make --batch-mode install -P release
```

To complete setup, download the LIRICAL resources and compile.

```
$ java -jar lirical-cli/target/lirical-cli-2.0.0-SNAPSHOT.jar download
$ ./mvnw clean compile
```
