package com.example.demo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManagerImpl;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    SparqlDataService sparqlDataService;

    private Dialog dialog = new Dialog(new Span("Error! No query string was provided, please try again."));
    private Button closeButton = new Button("Close", e -> dialog.close());
    private Checkbox negateCheckBox = new Checkbox("Not", false);

    private TextField query = new TextField("Query");
    private TextField result = new TextField();
    private TextField meaning = new TextField();
    private TextArea partOfSpeech = new TextArea();

    private ArrayList<TextField> resultsList = new ArrayList<TextField>();
    private ArrayList<TextField> meaningsList = new ArrayList<TextField>();
    private ArrayList<TextArea> partOfSpeechesList = new ArrayList<TextArea>();
    private ComboBox<String> searchIn = new ComboBox<>("Search in");

    private Grid<SparqlData> grid = new Grid<>(SparqlData.class);
    private Binder<SparqlData> binder = new Binder<>(SparqlData.class);

    static final String inputFileName  = "dictionary.rdf";

    public MainView(SparqlDataService service) {
        this.sparqlDataService = service;

        addItemsSearchIn(searchIn);
        grid.setColumns("result", "partOfSpeech");
        grid.addColumn(
                        TemplateRenderer.<SparqlData>of("<div style='white-space:normal'>[[item.meaning]]</div>")
                                .withProperty("meaning", SparqlData::getMeaning))
                .setHeader("Meaning").setFlexGrow(1);
        add(getForm(), grid);

        refreshGrid();
    }
    private Component getForm() {

        var nextPageButton = new Button("Word creation page");

        var layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.BASELINE);
        query.setWidth("13em");
        query.setPlaceholder("Type word or a fragment");
        var queryButton = new Button("Run query");
        queryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var showAll = new Button("Show all dictionary");
        showAll.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var clearButton = new Button("Clear results");
        clearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(query, negateCheckBox, searchIn, queryButton, showAll, clearButton, nextPageButton);

        binder.bindInstanceFields(this);

        queryButton.addClickListener(click -> {
            try {
                sparqlDataService.deleteAll();
                if (query.getValue().isBlank()) {
                    dialog.getFooter().add(closeButton);
                    add(dialog);
                    dialog.open();
                    /* position the dialog next to the button on the left */
                    return;
                }
                if (query.getValue().isEmpty()) {
                    sparqlShowAll();
                } else {
                    sparqlSearch(query.getValue());
                }
                for (int i=0; i < resultsList.size(); i++) {
                    result.setValue(resultsList.get(i).getValue());
                    meaning.setValue(meaningsList.get(i).getValue());
                    partOfSpeech.setValue(partOfSpeechesList.get(i).getValue());

                    var sparqlData = new SparqlData();

                    binder.writeBean(sparqlData);
                    sparqlDataService.addData(sparqlData);
                    // repository.save(sparqlData);
                }
                resultsList.clear();
                meaningsList.clear();
                partOfSpeechesList.clear();
                refreshGrid();

            } catch (ValidationException e) {

            }
        });
        showAll.addClickListener(click -> {
            try {
                sparqlDataService.deleteAll();
                refreshGrid();
                Model model = ModelFactory.createDefaultModel();

                // use the RDFDataMgr to find the input file
                InputStream in = RDFDataMgr.open( inputFileName );
                if (in == null) {
                    throw new IllegalArgumentException(
                            "File: " + inputFileName + " not found");
                }

                // read the RDF/XML file
                model.read(in, null);

                // model.listSubjects();

                // select all the resources with a RDFS property
                ResIterator iter = model.listSubjectsWithProperty(RDFS.label);
                if (iter.hasNext()) {
                    while (iter.hasNext()) {
                        Resource resource = iter.nextResource();

                        result.setValue(resource
                                .getProperty(RDFS.label)
                                .getString());
                        meaning.setValue(resource
                                .getProperty(RDFS.comment)
                                .getString());
                        partOfSpeech.setValue(resource
                                .getProperty(RDFS.isDefinedBy)
                                .getString());

                        var sparqlData = new SparqlData();
                        binder.writeBean(sparqlData);
                        sparqlDataService.addData(sparqlData);

                    }
                } else {
                    System.out.println("No rdfs were found in the database");
                }

                refreshGrid();

            } catch (ValidationException e) {
                System.out.println("Error");
            }
        });


        clearButton.addClickListener(click -> {
            sparqlDataService.deleteAll();
            refreshGrid();
        });

        nextPageButton.addClickListener(click -> {
            UI.getCurrent().navigate("/create");
        });

        return layout;
    }

    private void refreshGrid() {
        grid.setItems(sparqlDataService.findAllData());
    }

    void sparqlSearch(String search) {
        String newSearch = "";
        if (search.contains("?") || search.contains("*")) {
            newSearch = SearchFunctions.searchToRegex(search);
        } else {
            newSearch = search;
        }
        String searchType;
        if (searchIn.getValue() == "Meaning") {
            searchType = "?meaning";
        } else {
            searchType = "?word";
        }
        if (newSearch.contains("{") && newSearch.contains("}")) {
            newSearch = SearchFunctions.addXWordContext(newSearch);
        }
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/SPARQL_Dictionary/dictionary.rdf");
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?word ?meaning ?partOW WHERE { " +
                // " ?x " + searchType + " \"" + search + "\" . " +
                " ?x rdfs:label ?word . " +
                " ?x rdfs:comment ?meaning . " +
                " ?x rdfs:isDefinedBy ?partOW . " +
                " FILTER (" + (negateCheckBox.getValue() ? "!" : "") + "regex(" + searchType + ", \"" + "^" + newSearch + "$" + "\", \"i\")) " +
                "}";
        Query query1 = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query1, model);
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("word");
                Literal meaning = soln.getLiteral("meaning");
                Literal partOW = soln.getLiteral("partOW");
                // System.out.println(name);
                TextField newVal = new TextField();
                TextField newVal2 = new TextField();
                TextArea newTxtArea = new TextArea();
                newVal.setValue(name.getString());
                resultsList.add(newVal);
                newVal2.setValue(meaning.getString());
                meaningsList.add(newVal2);
                newTxtArea.setValue(partOW.getString());
                partOfSpeechesList.add(newTxtArea);
            }
        }
        finally {
            qexec.close();
        }
    }

    public void sparqlQuery() {

    }

    public void sparqlShowAll() {
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/SPARQL_Dictionary/dictionary.rdf");
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?word ?meaning ?partOW WHERE { " +
                " ?x rdfs:label ?word . " +
                " ?x rdfs:comment ?meaning . " +
                " ?x rdfs:isDefinedBy ?partOW . " +
                "}";
        Query query1 = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query1, model);
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("word");
                Literal meaning = soln.getLiteral("meaning");
                Literal partOW = soln.getLiteral("partOW");
                // System.out.println(name);
                TextField newVal = new TextField();
                TextField newVal2 = new TextField();
                TextArea newTxtArea = new TextArea();
                newVal.setValue(name.getString());
                resultsList.add(newVal);
                newVal2.setValue(meaning.getString());
                meaningsList.add(newVal2);
                newTxtArea.setValue(partOW.getString());
                partOfSpeechesList.add(newTxtArea);
            }
        } finally {
            qexec.close();
        }
    }
    static void sparqlTest2() {
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/Jena_app/src/com/company/data.rdf");

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT * WHERE { " +
                " ?person foaf:name ?x ." +
                " ?person foaf:knows ?person2 ." +
                " ?person2 foaf:name ?y ." +
                "FILTER( ?y = \"vardas\")" +
                "}";
        org.apache.jena.query.Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("x");
                System.out.println(name);
            }
        }
        finally {
            qexec.close();
        }

    }
    static void sparqlTest3() {
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/Jena_app/src/com/company/data.rdf");

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT * WHERE { " +
                " ?person foaf:name ?x ." +
                " ?person foaf:knows ?person2 ." +
                " ?person2 foaf:name ?y ." +
                "}";
        org.apache.jena.query.Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("x");
                System.out.println(name);
            }
        }
        finally {
            qexec.close();
        }
    }
    public void addItemsSearchIn(ComboBox<String> searchIn) {
        List<String> searchesIn = new ArrayList<>();
        searchesIn.add("Word");
        searchesIn.add("Meaning");
        searchIn.setItems(searchesIn);
    }
}
