# L4CI
LIRICAL for clinical intuition



mvn archetype:generate \
-DarchetypeGroupId=org.monarchinitiative.l2ci \
-Dfilter=pom-root \
-DarchetypeVersion=RELEASE

## Making the mondo.json file

L4CI reads the Mondo ontology as a JSON file. The mondo in OWL format can be converted to a JSON file using obographs by typing the following into the Terminal:

```obographs convert mondo.owl -f json```
