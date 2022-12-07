package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.core.io.MondoNDescendantsMapIO;
import org.monarchinitiative.l2ci.core.io.OmimMapIO;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class MondoOmimTask extends Task<MondoOmim> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MondoOmimTask.class);

    private static final String OMIM_2_MONDO_NAME = "omim2mondo.csv";
    private static final String MONDO_N_DESC_NAME = "mondoDesc.csv";

    private final Ontology mondo;
    private final Path dataDirectory;

    public MondoOmimTask(Ontology mondo, Path dataDirectory) {
        this.mondo = Objects.requireNonNull(mondo);
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected MondoOmim call() throws Exception {
        LOGGER.debug("Loading Mondo metadata using the data directory at {}", dataDirectory.toAbsolutePath());

        // Load the OMIM to Mondo from the cache file if present, or compute and cache the map.
        Path omimToMondoPath = dataDirectory.resolve(OMIM_2_MONDO_NAME);
        Map<TermId, List<TermId>> omim2Mondo;
        if (Files.exists(omimToMondoPath)) {
            omim2Mondo = OmimMapIO.read(omimToMondoPath);
        } else {
            omim2Mondo = calculateOmimMap(mondo);
            OmimMapIO.write(omim2Mondo, omimToMondoPath);
        }

        // Flip the OMIM to Mondo map
        Map<TermId, TermId> mondoToOmim = new HashMap<>();
        for (Map.Entry<TermId, List<TermId>> e : omim2Mondo.entrySet()) {
            for (TermId mondoId : e.getValue()) {
                mondoToOmim.put(mondoId, e.getKey());
            }
        }

        // Same as above, load from cache or calculate and cache the map.
        Path mondoNDescendantsPath = dataDirectory.resolve(MONDO_N_DESC_NAME);
        Map<TermId, Integer> mondoNDescendents;
        if (Files.exists(mondoNDescendantsPath)) {
            mondoNDescendents = MondoNDescendantsMapIO.read(mondoNDescendantsPath);
        } else {
            mondoNDescendents = makeMondoDescendantsMaps(mondo, omim2Mondo);
            MondoNDescendantsMapIO.write(mondoNDescendents, mondoNDescendantsPath);
        }

        return new MondoOmim(omim2Mondo, Map.copyOf(mondoToOmim), mondoNDescendents);
    }

    private static Map<TermId, List<TermId>> calculateOmimMap(Ontology mondo) {
        Map<TermId, List<TermId>> builder = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {

            for (Dbxref ref : mondoTerm.getXrefs()) {
                String refName = ref.getName();
                if (refName.startsWith("OMIM:")) {
                    TermId omimId = TermId.of(refName);
                    builder.computeIfAbsent(omimId, i -> new ArrayList<>())
                            .add(mondoTerm.id());
                    break;
                    // TODO(mabeckwith) - do we need to do this?
//                    mainController.omimLabelsAndMondoTermIdMap.put(omimTerm.id().toString(), mondoTerm.id());
                }
            }
        }
        return Map.copyOf(builder);
    }


    private static Map<TermId, Integer> makeMondoDescendantsMaps(Ontology mondo, Map<TermId, List<TermId>> omim2Mondo) {
//        String[] probMondos = {"MONDO:0000001","MONDO:0000252","MONDO:0000257","MONDO:0000432","MONDO:0000888","MONDO:0000916", "MONDO:0001517","MONDO:0001673",
//                "MONDO:0002051", "MONDO:0002081","MONDO:0002269","MONDO:0002320","MONDO:0002334","MONDO:0002525","MONDO:0002602", "MONDO:0003847","MONDO:0003939",
//                "MONDO:0004095","MONDO:0004335","MONDO:0004805", "MONDO:0005020", "MONDO:0005027","MONDO:0005046","MONDO:0005062","MONDO:0005066","MONDO:0005070","MONDO:0005071","MONDO:0005093",
//                "MONDO:0005157","MONDO:0005218","MONDO:0005550","MONDO:0005559","MONDO:0005560","MONDO:0005570","MONDO:0005579","MONDO:0006547", "MONDO:0008945","MONDO:0011876", "MONDO:0013598",
//                "MONDO:0015286","MONDO:0015650","MONDO:0015757","MONDO:0019044","MONDO:0019052","MONDO:0019117","MONDO:0020573","MONDO:0020579","MONDO:0020683", "MONDO:0021125","MONDO:0021152",
//                "MONDO:0021166","MONDO:0024237", "MONDO:0042489","MONDO:0043424","MONDO:0044881", "MONDO:0100062","MONDO:0100079","MONDO:0100135","MONDO:0100455", "MONDO:0700092"};
//        Integer[] probMondoNDesc = {0,27,27,4,54,27,27,16,2582,1190,55,1088,253,353,3853,0,940,28,731,116,266,631,1109,54,4433,1640,13134,2006,87,5,222,971,2562,1092,115,
//                1,0,1,0,311,336,59,84,2202,4460,0,55,71,0,0,226,553,0,29,133,98,0,0,20,237};
//        Map<String, Integer> probMondoDescMap = new HashMap<>();
//        for (int t=0; t<probMondos.length; t++) {
//            probMondoDescMap.put(probMondos[t], probMondoNDesc[t]);
//        }

        Map<TermId, Integer> builder = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {
            // TODO(mabeckwith) - update the code.
            Map<TermId, TermId> selectedTerms = new HashMap<>();
            TermId mondoID = mondoTerm.id();
            boolean doRefs = true;
//            if (probMondoDescMap.containsKey(mondoID.toString())) {
//                mainController.mondoNDescendantsMap.put(mondoID, probMondoDescMap.get(mondoID.toString()));
//                doRefs = false;
//            }
            if (doRefs) {
                List<Dbxref> mondoTermXRefs = mondoTerm.getXrefs();
                for (Dbxref ref : mondoTermXRefs) {
//                    Set<Term> descendents = mainController.getTermRelations(mondoID, Relation.DESCENDENT);
                    Set<Term> descendents = Set.of();
                    int nDescendents = 0;
                    for (Term descendent : descendents) {
                        for (TermId omimID : omim2Mondo.keySet()) {
                            List<TermId> mondoIDs = omim2Mondo.get(omimID);
                            if (mondoIDs.contains(descendent.id())) {
                                selectedTerms.put(omimID, descendent.id());
                                if (!descendent.id().equals(mondoID)) {
                                    nDescendents++;
                                }
                                break;
                            }
                        }
                    }
                    builder.put(mondoID, nDescendents);
                    break;
                }
            }
        }

        return Map.copyOf(builder);
    }

//
//
//    private void loadMondoDescMapFile(File mondoDescMapFile) {
//        LOGGER.info("Reading Mondo Descendants Map from " + mondoDescMapFile.getAbsolutePath());
//        try (InputStream is = Files.newInputStream(mondoDescMapFile.toPath())) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//            List<String> lines = reader.lines().toList();
//            for (String line : lines) {
//                if (line.contains("Descendants")) {
//                    continue;
//                }
//                Map<TermId, TermId> descMap = new HashMap<>();
//                String[] lineItems = line.split(",");
//                if (lineItems.length == 0) {
//                    lineItems = new String[]{line};
//                }
//                for (String item : lineItems) {
//                    if (item.contains("OMIM") & item.contains("MONDO")) {
//                        String[] itemSplit = item.split(": ");
//                        Term omimTerm = Term.of(itemSplit[0], itemSplit[0]);
//                        Term mondoTerm = Term.of(itemSplit[1], itemSplit[1]);
//                        descMap.put(omimTerm.id(), mondoTerm.id());
//                    }
//                }
//                TermId mondoId = Term.of(lineItems[0], lineItems[0]).id();
//                MainController.mondoDescendantsMap.put(mondoId, descMap);
//            }
//            reader.close();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

//    private void loadMondoNDescMapFile(File mondoNDescMapFile) {
//        LOGGER.info("Reading Mondo No. Descendants Map from " + mondoNDescMapFile.getAbsolutePath());
//        try (InputStream is = Files.newInputStream(mondoNDescMapFile.toPath())) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//            List<String> lines = reader.lines().toList();
//            for (String line : lines) {
//                String[] lineItems = line.split(",");
//                if (lineItems[1].contains("Descendants")) {
//                    continue;
//                }
//                TermId omimId = Term.of(lineItems[0], lineItems[0]).id();
//                MainController.mondoNDescendantsMap.put(omimId, Integer.parseInt(lineItems[1]));
//            }
//            reader.close();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

}
