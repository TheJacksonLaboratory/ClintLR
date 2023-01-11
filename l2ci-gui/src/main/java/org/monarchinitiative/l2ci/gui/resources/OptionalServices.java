package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.springframework.stereotype.Component;

@Component
public class OptionalServices {

    private final ObjectProperty<Lirical> lirical = new SimpleObjectProperty<>(this, "lirical");
    private final ObjectProperty<Ontology> mondo = new SimpleObjectProperty<>(this, "mondo");
    private final MondoOmimResources mondoOmimResources = new MondoOmimResources();

    public Lirical getLirical() {
        return lirical.get();
    }

    public ObjectProperty<Lirical> liricalProperty() {
        return lirical;
    }

    public void setLirical(Lirical lirical) {
        this.lirical.set(lirical);
    }

    public Ontology getMondo() {
        return mondo.get();
    }

    public ObjectProperty<Ontology> mondoProperty() {
        return mondo;
    }

    public void setMondo(Ontology mondo) {
        this.mondo.set(mondo);
    }

    public MondoOmimResources mondoOmimResources() {
        return mondoOmimResources;
    }
}
