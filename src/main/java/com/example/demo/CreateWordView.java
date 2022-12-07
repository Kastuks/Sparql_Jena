package com.example.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.jena.rdf.model.Property;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

class foaf {
    public static final String uri = "http://xmlns.com/foaf/0.1/";

    private static final Model m = ModelFactory.createDefaultModel();
    public static final Property PERSON;
    public static final Property FAMILY_NAME;

    public static String getURI() {
        return "http://xmlns.com/foaf/0.1/";
    }

    static {
        PERSON = m.createProperty("http://xmlns.com/foaf/0.1/#term_Person");
        FAMILY_NAME = m.createProperty("http://xmlns.com/foaf/0.1/#term_family_name");
    }
}

@Route("/create")
public class CreateWordView extends VerticalLayout {

    public static final String RDFuri = "http://xmlns.com/foaf/0.1/";
    public static final String RDFSuri = "http://xmlns.com/foaf/0.1/";
    public static String getRDFURI() {
        return RDFuri;
    }
    public static String getRDFSURI() {
        return RDFSuri;
    }

    @PersistenceContext
    private EntityManager em;

    @Autowired
    SparqlDataService sparqlDataService;

    @Autowired
    private SparqlDataRepository repository;

    private TextField word = new TextField("Word");
    private TextField meaning = new TextField("Meaning");
    private TextField result = new TextField();
    private ArrayList<TextField> resultsList = new ArrayList<TextField>();
    private Binder<SparqlData> binder = new Binder<>(SparqlData.class);
    static final String inputFileName  = "filename.rdf";

    public CreateWordView(SparqlDataService service) {
        this.sparqlDataService = service;
        add(getDataForm());

    }
    private Component getDataForm() {

        var layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);

        var addButton = new Button("Add to dictionary");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var redirectButton = new Button("Back to main page");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(word, meaning, addButton, redirectButton);

        binder.bindInstanceFields(this);

        addButton.addClickListener(click -> {
            try {
                String defaultURI    = "http://somewhere/";

                // create an empty model
                Model model = ModelFactory.createDefaultModel();

                // use the RDFDataMgr to find the input file
                InputStream in = RDFDataMgr.open( inputFileName );
                if (in == null) {
                    throw new IllegalArgumentException(
                            "File: " + inputFileName + " not found");
                }

                // read the RDF/XML file
                model.read(in, null);

                if (word.getValue() != null) {
                    Resource res = model.createResource(defaultURI + word.getValue())
                            .addProperty(RDFS.label, word.getValue())
                            .addProperty(RDFS.comment, meaning.getValue());
                }

                try {
                    FileWriter myObj = new FileWriter(inputFileName);
                    model.write(myObj);

                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }

                // // create the resource
                // //   and add the properties cascading style
                // Resource johnSmith
                //         = model.createResource(personURI)
                //         .addProperty(VCARD.FN, fullName)
                //         .addProperty(VCARD.N,
                //                 model.createResource()
                //                         .addProperty(VCARD.Given, givenName)
                //                         .addProperty(VCARD.Family, familyName));

                // // now write the model in XML form to a file
                // Resource vcard = model.getResource(personURI);
                // // retrieve the value of the N property
                // Resource name = (Resource) vcard.getProperty(VCARD.N)
                //         .getObject();
                // model.write(System.out);
                // System.out.println(vcard.getProperty(VCARD.FN).getString());
                // Model model2 = ModelFactory.createDefaultModel();
                //
                // InputStream in = FileManager.get().open( inputFileName );
                // if (in == null) {
                //     throw new IllegalArgumentException( "File: " + inputFileName + " not found");
                // }
                //
                // // read the RDF/XML file
                // model2.read(in, "");
                // model2.write(System.out);
                //
                // // Resource foaf = model2.getResource("Kes Tas");
                // Resource person = model2.getResource("file:///C:/stud/workspaces/demo/#me");
                // String sv = person.getProperty(foaf.PERSON).getResource().getProperty(foaf.FAMILY_NAME).getString();
                // System.out.println(person.getProperty(foaf.PERSON).getResource().getProperty(foaf.FAMILY_NAME).getString());

                add("Word added to RDF");


            } catch (Exception e) {

            }
        });
        redirectButton.addClickListener(click -> {
            UI.getCurrent().navigate("");
        });
        return layout;
    }

}
