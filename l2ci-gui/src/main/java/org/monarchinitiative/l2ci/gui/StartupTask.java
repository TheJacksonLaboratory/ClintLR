package org.monarchinitiative.l2ci.gui;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.core.Relation;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.io.MondoDescendantsMapFileWriter;
import org.monarchinitiative.l2ci.core.io.OmimMapFileWriter;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Initialization of the GUI resources is being done here. Information from {@link Properties} parsed from
 * <code>hpo-case-annotator.properties</code> are being read and following resources are initialized:
 * <ul>
 * <li>Human phenotype and Mondo ontology JSON files</li>
 * </ul>
 * <p>
 * Changes made by user are stored for the next run in
 *
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 2.0.0
 * @since 0.0
 */
public final class StartupTask extends Task<Void> {

    private final MainController mainController = MainController.getController();
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupTask.class);

    private final OptionalHpoResource optionalHpoResource;

    private final OptionalHpoaResource optionalHpoaResource;

    private final OptionalMondoResource optionalMondoResource;

    private final Properties pgProperties;

    private enum Type {
        HPO("HPO"),
        HPOA("phenotype.hpoa"),
        Mondo("Mondo");

        private final String name;

        Type(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public StartupTask(OptionalHpoResource hpoResource,
                       OptionalHpoaResource hpoaResource,
                       OptionalMondoResource mondoResource, Properties pgProperties) {
        this.pgProperties = pgProperties;
        this.optionalHpoResource = hpoResource;
        this.optionalHpoaResource = hpoaResource;
        this.optionalMondoResource = mondoResource;
    }

    /**
     * Read {@link Properties} and initialize app resources in the :
     *
     * <ul>
     * <li>HPO ontology</li>
     * </ul>
     *
     * @return nothing
     */
    @Override
    protected Void call() {
        /*
        This is the place where we deserialize HPO ontology if we know path to the OBO file.
        We need to make sure to set ontology property of `optionalResources` to null if loading fails.
        This way we ensure that GUI elements dependent on ontology presence (labels, buttons) stay disabled
        and that the user will be notified about the fact that the ontology is missing.
         */
        String hpoJsonPath = pgProperties.getProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY);
        String hpoAnnotPath = pgProperties.getProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY);
        String mondoJsonPath = pgProperties.getProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY);
        updateProgress(0.02, 1);
        String[] paths = {mondoJsonPath, hpoJsonPath, hpoAnnotPath};
        Type[] types = {Type.Mondo, Type.HPO, Type.HPOA};
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            Type type = types[i];
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    String msg = String.format("Loading " + type + " from file '%s'", file.getAbsoluteFile());
                    updateMessage(msg);
                    LOGGER.info(msg);
                    switch (type) {
                        case Mondo:
                            HPOParser parser = new HPOParser(mondoJsonPath);
                            Ontology ontology = parser.getHPO();
                            setOntology(type, ontology, "");
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case HPO:
                            ontology = OntologyLoader.loadOntology(file);
                            optionalHpoResource.setOntology(ontology);
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case HPOA:
                            if (optionalHpoResource.getOntology() == null) {
                                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
                                return null;
                            }
                            setOntology(type, optionalHpoResource.getOntology(), path);
                            LOGGER.info("Loaded annotation maps");
                            break;
                        }
                    updateProgress((double) (i+1)/paths.length, 1);
                    updateMessage(type + " loaded");
                } else {
                    setOntology(type, null, "");
                }
                LOGGER.info("Finished loading " + type + " ontology.");
            } else {
                String msg = "Need to set path to " + type + " file (See File -> Show Resources menu)";
                updateMessage(msg);
                LOGGER.info(msg);
                setOntology(type, null, "");
            }
        }
        String homeDir = new File(".").getAbsolutePath();
        String[] dir = {homeDir.substring(0, homeDir.length() - 2), "l2ci-gui", "src", "main", "resources", "omim2mondoMap.txt"};
        String path = String.join(File.separator, dir);
        File omimMapFile = new File(path);
        if (!omimMapFile.isFile()) {
            String msg = "Making Omim to Mondo Map.";
            updateMessage(msg);
            makeOmimMap();
            saveOmimMapToFile(omimMapFile);
        } else {
            loadOmimMapFile(omimMapFile);
        }
        updateProgress(0.8, 1);
        dir = new String[]{homeDir.substring(0, homeDir.length() - 2), "l2ci-gui", "src", "main", "resources", "mondoNDescMap.txt"};
        path = String.join(File.separator, dir);
        File mondoNDescMapFile = new File(path);
        if (!mondoNDescMapFile.isFile()) {
            String msg = "Making Mondo descendants Map.";
            updateMessage(msg);
            makeMondoNDescendantsMap();
            saveMondoNDescMapToFile(mondoNDescMapFile);
        } else {
            loadMondoNDescMapFile(mondoNDescMapFile);
        }
        updateProgress(1, 1);
        return null;
    }

    void setOntology(Type type, Ontology ont, String path) {
        switch (type) {
            case Mondo:
                optionalMondoResource.setOntology(ont);
                break;
            case HPO:
                optionalHpoResource.setOntology(ont);
                break;
            case HPOA:
                if (ont != null) {
                    optionalHpoaResource.setAnnotationResources(path, ont);
                } else {
                    optionalHpoaResource.initializeWithEmptyMaps();
                }
                break;
        }
    }

    private void makeOmimMap() {
        Ontology ontology = optionalMondoResource.getOntology();
        if (ontology == null) {
            LOGGER.error("makeOmimMap: ontology is null.");
        } else {
            for (Term mondoTerm : ontology.getTerms()) {
                for (Dbxref ref : mondoTerm.getXrefs()) {
                    String refName = ref.getName();
                    if (refName.contains("OMIM:")) {
                        Term omimTerm = Term.of(refName, refName);
                        TermId omimID = omimTerm.id();
                        if (!mainController.omimToMondoMap.containsKey(omimID)) {
                            mainController.omimToMondoMap.put(omimID, new ArrayList<>());
                        }
                        List<TermId> termList = mainController.omimToMondoMap.get(omimID);
                        termList.add(mondoTerm.id());
                        mainController.omimToMondoMap.put(omimID, termList);
                        mainController.omimLabelsAndMondoTermIdMap.put(omimTerm.id().toString(), mondoTerm.id());
                        break;
                    }
                }
            }
        }
    }

    private void saveOmimMapToFile(File omimMapFile) {
        new OmimMapFileWriter(mainController.omimToMondoMap, omimMapFile.getAbsolutePath());
    }

    private void loadOmimMapFile(File omimMapFile) {
        LOGGER.info("Reading Omim to Mondo Map from " + omimMapFile.getAbsolutePath());
        try (InputStream is = Files.newInputStream(omimMapFile.toPath())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = reader.lines().toList();
            for (String line : lines) {
                List<TermId> mondoList = new ArrayList<>();
                String[] lineItems = line.split(",");
                if (lineItems[1].contains("Mondo Terms")) {
                    continue;
                }
                for (String item : lineItems) {
                    if (item.contains("MONDO")) {
                        mondoList.add(Term.of(item, item).id());
                    }
                }
                TermId omimId = Term.of(lineItems[0], lineItems[0]).id();
                mainController.omimToMondoMap.put(omimId, mondoList);
                mainController.omimLabelsAndMondoTermIdMap.put(omimId.toString(), mondoList.get(0));
            }
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void makeMondoNDescendantsMap() {
        Ontology ontology = optionalMondoResource.getOntology();
        List<Term> mondoTerms = ontology.getTerms().stream().toList();
        Set<TermId> omimIDs = mainController.omimToMondoMap.keySet();
        String[] probMondos = {"MONDO:0000001","MONDO:0000252","MONDO:0000257","MONDO:0000432","MONDO:0000888","MONDO:0000916", "MONDO:0001517","MONDO:0001673",
                "MONDO:0002051", "MONDO:0002081","MONDO:0002269","MONDO:0002320","MONDO:0002334","MONDO:0002525","MONDO:0002602", "MONDO:0003847","MONDO:0003939",
                "MONDO:0004095","MONDO:0004335","MONDO:0004805", "MONDO:0005020", "MONDO:0005027","MONDO:0005046","MONDO:0005062","MONDO:0005066","MONDO:0005070","MONDO:0005071","MONDO:0005093",
                "MONDO:0005157","MONDO:0005218","MONDO:0005550","MONDO:0005559","MONDO:0005560","MONDO:0005570","MONDO:0005579","MONDO:0006547", "MONDO:0008945","MONDO:0011876", "MONDO:0013598",
                "MONDO:0015286","MONDO:0015650","MONDO:0015757","MONDO:0019044","MONDO:0019052","MONDO:0019117","MONDO:0020573","MONDO:0020579","MONDO:0020683", "MONDO:0021125","MONDO:0021152",
                "MONDO:0021166","MONDO:0024237", "MONDO:0042489","MONDO:0043424","MONDO:0044881", "MONDO:0100062","MONDO:0100079","MONDO:0100135","MONDO:0100455", "MONDO:0700092"};
        Integer[] probMondoNDesc = {0,27,27,4,54,27,27,16,2582,1190,55,1088,253,353,3853,0,940,28,731,116,266,631,1109,54,4433,1640,13134,2006,87,5,222,971,2562,1092,115,
                1,0,1,0,311,336,59,84,2202,4460,0,55,71,0,0,226,553,0,29,133,98,0,0,20,237};
        Map<String, Integer> probMondoDescMap = new HashMap<>();
        for (int t=0; t<probMondos.length; t++) {
            probMondoDescMap.put(probMondos[t], probMondoNDesc[t]);
        }

        for (Term mondoTerm : mondoTerms) {
            TermId mondoID = mondoTerm.id();
//            System.out.println("Mondo term " + mondoTerms.indexOf(mondoTerm) + " of " + mondoTerms.size());
            boolean doRefs = true;
            if (probMondoDescMap.containsKey(mondoID.toString())) {// && !mondoID.toString().contains("MONDO:0005071")) {
//                System.out.println(mondoID + ": " + mondoTerm.getName());
//                System.out.println(mondoID + " idx = " + probMondos.indexOf(mondoID.toString()));
                mainController.mondoNDescendantsMap.put(mondoID, probMondoDescMap.get(mondoID.toString()));
                doRefs = false;
            }
            if (doRefs) {
//                System.out.println(mondoTerm.id());
                List<Dbxref> mondoTermXRefs = mondoTerm.getXrefs();
                for (Dbxref ref : mondoTermXRefs) {

                    Set<Term> descendents = mainController.getTermRelations(mondoID, Relation.DESCENDENT);
                    int nDescendents = 0;
                    if (descendents.size() > 1) {
                        for (Term descendent : descendents) {
                            for (TermId omimID2 : omimIDs) {
                                List<TermId> mondoIDs2 = mainController.omimToMondoMap.get(omimID2);
                                if (mondoIDs2.contains(descendent.id())) {
                                    nDescendents++;
                                    break;
                                }
                            }
                        }
                    }
                    mainController.mondoNDescendantsMap.put(mondoID, nDescendents);
                    break;
                }
            }
        }
    }

    private void saveMondoNDescMapToFile(File mondoNDescMapFile) {
        new MondoDescendantsMapFileWriter(mainController.mondoNDescendantsMap, mondoNDescMapFile.getAbsolutePath());
    }

    private void loadMondoNDescMapFile(File mondoNDescMapFile) {
        LOGGER.info("Reading Mondo No. Descendants Map from " + mondoNDescMapFile.getAbsolutePath());
        try (InputStream is = Files.newInputStream(mondoNDescMapFile.toPath())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = reader.lines().toList();
            for (String line : lines) {
                String[] lineItems = line.split(",");
                if (lineItems[1].contains("Descendants")) {
                    continue;
                }
                TermId omimId = Term.of(lineItems[0], lineItems[0]).id();
                mainController.mondoNDescendantsMap.put(omimId, Integer.parseInt(lineItems[1]));
            }
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
