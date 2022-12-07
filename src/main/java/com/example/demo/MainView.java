package com.example.demo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.jena.base.Sys;
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
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManagerImpl;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    SparqlDataService sparqlDataService;

    @Autowired
    private SparqlDataRepository repository;

    private TextField query = new TextField("Query");
    private TextField result = new TextField();
    private TextField meaning = new TextField();
    private ArrayList<TextField> resultsList = new ArrayList<TextField>();
    private Grid<SparqlData> grid = new Grid<>(SparqlData.class);
    private Binder<SparqlData> binder = new Binder<>(SparqlData.class);
    static final String inputFileName  = "filename.rdf";

    // SparqlData sparqlData = new SparqlData();

    public MainView(SparqlDataService service) {
        this.sparqlDataService = service;
        // add(new H1("Hello world"));
        // var button = new Button("click me");
        // var textField = new TextField();
        // add(textField, button);

        // sparqlTest1();
        // sparqlTest2();

        grid.setColumns("result", "meaning");
        add(getForm(), grid);


        // button.addClickListener(e -> {
        //     add(new Paragraph("hello") + textField.getValue());
        //     textField.clear();
        // });
        refreshGrid();
    }
    private Component getForm() {
        // var horlayout = new HorizontalLayout();
        // horlayout.setAlignItems(Alignment.END);

        var nextPageButton = new Button("Word creation page");

        // horlayout.add(nextPageButton);
        var layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.BASELINE);

        var addButton = new Button("Run query");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var showAll = new Button("Show all dictionary");
        var clearButton = new Button("Clear results");
        clearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(query, addButton, showAll, clearButton, nextPageButton);

        binder.bindInstanceFields(this);

        addButton.addClickListener(click -> {
            try {
                sparqlTest12(query.getValue());
                for (TextField res : resultsList) {
                    result.setValue(res.getValue());
                    var sparqlData = new SparqlData();

                    binder.writeBean(sparqlData);
                    sparqlDataService.addData(sparqlData);
                    // repository.save(sparqlData);

                }
                resultsList.clear();
                refreshGrid();

            } catch (ValidationException e) {

            }
        });
        showAll.addClickListener(click -> {
            try {
                Model model = ModelFactory.createDefaultModel();

                // use the RDFDataMgr to find the input file
                InputStream in = RDFDataMgr.open( inputFileName );
                if (in == null) {
                    throw new IllegalArgumentException(
                            "File: " + inputFileName + " not found");
                }

                // read the RDF/XML file
                model.read(in, null);

                model.listSubjects();

                // select all the resources with a RDFS property
                ResIterator iter = model.listSubjectsWithProperty(RDFS.label);
                ResIterator iter2 = model.listSubjectsWithProperty(RDFS.label);

                if (iter.hasNext()) {
                    while (iter.hasNext()) {
                        TextField res = new TextField();
                        res.setValue(iter.nextResource()
                                .getProperty(RDFS.label)
                                .getString());
                        result.setValue(res.getValue());
                        res.setValue(iter2.nextResource()
                                .getProperty(RDFS.comment)
                                .getString());
                        meaning.setValue(res.getValue());

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
           // grid.setItems([]);
            sparqlDataService.deleteAll();
            // repository.deleteAll();

            refreshGrid();
        });

        nextPageButton.addClickListener(click -> {
            UI.getCurrent().navigate("/create");
        });

        return layout;
    }

    private void refreshGrid() {
        grid.setItems(sparqlDataService.findAllData());
        // sparqlTest12(query.getValue());
    }

    void sparqlTest12(String foaf) {
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/demo/filename.rdf");
        String rezultatas;
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT * WHERE { " +
                " ?person foaf:" + foaf + " ?x ." +
                "}";
        Query query1 = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query1, model);

        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("x");
                TextField newVal = new TextField();
                newVal.setValue(name.getString());
                resultsList.add(newVal);
                result.setValue(name.getString());
                System.out.println(name);
            }
        }
        finally {
            qexec.close();
        }

    }

    public void sparqlTest1() {
        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel("c:/stud/workspaces/Jena_app/src/com/company/data.rdf");

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT * WHERE { " +
                " ?person foaf:name ?x ." +
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

}
