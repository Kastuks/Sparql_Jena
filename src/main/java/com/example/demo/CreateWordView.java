package com.example.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.jena.rdf.model.Property;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

@Route("/create")
public class CreateWordView<Planet> extends VerticalLayout {

    public static final String RDFuri = "http://xmlns.com/foaf/0.1/";
    public static final String RDFSuri = "http://xmlns.com/foaf/0.1/";
    private static final String defaultURI = "http://somewhere/";

    public static String getRDFURI() {
        return RDFuri;
    }

    public static String getRDFSURI() {
        return RDFSuri;
    }

    private static final String inputFileName = "dictionary.rdf";

    @PersistenceContext
    private EntityManager em;

    @Autowired
    SparqlDataService sparqlDataService;

    @Autowired
    private SparqlDataRepository repository;
    List<Planet> planets = new ArrayList<>();
    private TextField word = new TextField("Word");
    private ComboBox<String> partOfSpeech = new ComboBox<>("Part Of Speech");
    private TextArea meaning = new TextArea("Meaning");
    private TextField result = new TextField();
    private ArrayList<TextField> resultsList = new ArrayList<TextField>();
    private Binder<SparqlData> binder = new Binder<>(SparqlData.class);
    private Text message = new Text("Error");

    public CreateWordView(SparqlDataService service) {
        this.sparqlDataService = service;
        addItems(partOfSpeech);
        add(getDataForm());
    }

    private Component getDataForm() {
        var layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);

        var addButton = new Button("Add to dictionary");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var redirectButton = new Button("Back to main page");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(word, partOfSpeech, meaning, addButton, redirectButton);

        binder.bindInstanceFields(this);

        addButton.addClickListener(click -> {

            try {
                if (message.getText() != "Error") {
                    remove(message);
                }
                if (word.getValue().isEmpty()) {
                    message.setText("Missing word, please try again.");
                    add(message);
                    return;
                } else if (partOfSpeech.getValue() == null) {
                    message.setText("Missing part of speech, please try again.");
                    add(message);
                    return;
                } else if (meaning.getValue().isEmpty()) {
                    message.setText("Missing meaning, please try again.");
                    add(message);
                    return;
                }

                // create an empty model
                Model model = ModelFactory.createDefaultModel();

                try {
                    if ((new File(inputFileName).length() == 0)) {
                        FileWriter myObj = new FileWriter(inputFileName);

                        if (word.getValue() != null) {
                            word.getValue().replace(" ", "_");
                            Resource res = model.createResource(defaultURI + word.getValue() + partOfSpeech.getValue()).addProperty(RDFS.label, word.getValue()).addProperty(RDFS.isDefinedBy,
                                    partOfSpeech.getValue()).addProperty(RDFS.comment, meaning.getValue());
                        }
                        model.write(myObj);

                    } else {
                        InputStream in = RDFDataMgr.open(inputFileName);
                        if (in == null) {
                            throw new IllegalArgumentException("File: " + inputFileName + " not found");
                        }

                        // read the RDF/XML file
                        model.read(in, null);

                        if (word.getValue() != null) {
                            ResIterator iter = model.listSubjects();
                            boolean check = false;
                            while (iter.hasNext()) {
                                Resource resource = iter.nextResource();
                                if (resource.getProperty(RDFS.label).getString().equals(word.getValue()) && resource.getProperty(RDFS.isDefinedBy).getString().equals(partOfSpeech.getValue())) {
                                    check = true;
                                }
                            }
                            if (check) {
                                message.setText("This word was already added to the dictionary, please try again.");
                                add(message);
                                return;
                            }
                            Resource res = model.createResource(defaultURI + word.getValue() + partOfSpeech.getValue()).addProperty(RDFS.label, word.getValue()).addProperty(RDFS.isDefinedBy,
                                    partOfSpeech.getValue()).addProperty(RDFS.comment, meaning.getValue());
                        }
                        FileWriter myObj = new FileWriter(inputFileName);
                        model.write(myObj);
                    }

                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                message.setText("Word added to RDF");
                add(message);

            } catch (Exception e) {
                message.setText("An error occurred");
                add(message);
                System.out.println("An error occurred");
            }
        });
        redirectButton.addClickListener(click -> {
            UI.getCurrent().navigate("");
        });
        return layout;
    }

    public void addItems(ComboBox<String> partOfSpeech) {
        List<String> partsOfSpeech = new ArrayList<>();
        partsOfSpeech.add("Noun");
        partsOfSpeech.add("Verb");
        partsOfSpeech.add("Adjective");
        partsOfSpeech.add("Pronoun");
        partsOfSpeech.add("Adverb");
        partsOfSpeech.add("Preposition");
        partsOfSpeech.add("Conjunction");
        partsOfSpeech.add("Interjection");
        partOfSpeech.setItems(partsOfSpeech);
    }
}
