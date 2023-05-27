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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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
    private ComboBox<LogicalOperatorEnum> logicalOperator = new ComboBox<>();
    private String currentDirectory = System.getProperty("user.dir");

    private TextField query = new TextField("Query");
    private TextField result = new TextField();
    private TextField meaning = new TextField();
    private TextArea partOfSpeech = new TextArea();

    private TextField query2 = new TextField("Query");
    private ComboBox<String> searchIn2 = new ComboBox<>("Search in");

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
        addItemsSearchIn(searchIn2);
        grid.setColumns("result", "partOfSpeech");
        grid.addColumn(
                        TemplateRenderer.<SparqlData>of("<div style='white-space:normal'>[[item.meaning]]</div>")
                                .withProperty("meaning", SparqlData::getMeaning))
                .setHeader("Meaning").setFlexGrow(1);


        add(getForm(), getForm2(), grid);

        refreshGrid();
    }

    private Component getForm2() {
        var layout = new HorizontalLayout();
        var informationButton = new Button("Tutorial");

        addItemsLogicalOperator(logicalOperator);
        logicalOperator.setValue(LogicalOperatorEnum.OR);
        logicalOperator.setWidth("5em");

        informationButton.getElement().getStyle().set("margin-left", "auto");
        layout.setWidthFull();
        layout.setAlignItems(Alignment.BASELINE);
        query2.setWidth("13em");
        query2.setPlaceholder("Type word or a fragment");
        layout.add(query2, searchIn2, logicalOperator, informationButton);
        searchIn2.setEnabled(false);

        informationButton.addClickListener(click -> {
            String spanText = "How to use this web dictionary: <br>" +
                    "Select AND or OR to use query for a match between either of the search bars or for a search to match "
                    + "both search bars in the given dictionary. A search may be performed for the meaning of a word or the word itself. "
                    + "Select a value in the top \"Search in\" combo box to search for the meaning of the word or the word itself. "
                    + "The \"Not\" checkbox may be checked in order to get results that are opposite of the selected query. "
                    + "<br> There are also search functions implemented into this web dictionary: <br>"
                    + "* - Use the star symbol in order to search for any multiple characters ([a-Z], word boundary). \"def*\" would be a correct match of \"definition\"<br> "
                    + "? - Use the question mark in order to search for any single character that is not a whitespace. \"defin?\" would match \"define\", but not \"defines\".<br> "
                    + "<X> word context - Use the <X> (X - number) to skip the selected amount of words in a string of text. "
                    + "For example, the given search query: \"enclose <10> lines.\" would match the text: \"enclose (a piece of text) within printed lines\". <br>"
                    + "These search functions may be used together.";

            Span infoSpan = new Span();
            infoSpan.getElement().setProperty("innerHTML", spanText);
            Dialog infoDialog = new Dialog(infoSpan);
            Button closeButton2 = new Button("Close", e -> infoDialog.close());

            infoDialog.setHeaderTitle("How to use search");
            infoDialog.getFooter().add(closeButton2);

            add(infoDialog);
            infoDialog.open();
        });

        return layout;
    }

    private Component getForm() {
        var layout = new HorizontalLayout();
        var nextPageButton = new Button("Word creation page");
        var queryButton = new Button("Run query");
        var showAll = new Button("Show all dictionary");
        var clearButton = new Button("Clear results");

        queryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        showAll.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        clearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.BASELINE);
        query.setWidth("13em");
        query.setPlaceholder("Type word or a fragment");

        nextPageButton.getElement().getStyle().set("margin-left", "auto");

        layout.add(query, searchIn, negateCheckBox, queryButton, showAll, clearButton, nextPageButton);
        layout.setAlignSelf(Alignment.END, nextPageButton);

        binder.bindInstanceFields(this);

        searchIn.addValueChangeListener(event -> {
            if (searchIn.getValue().equals(SearchInEnum.Word.name())) {
                searchIn2.setValue(SearchInEnum.Meaning.name());
            } else {
                searchIn2.setValue(SearchInEnum.Word.name());
            }
        });

        queryButton.addClickListener(click -> {
            try {
                sparqlDataService.deleteAll();

                if (query.getValue().isEmpty() && query2.getValue().isEmpty()) {
                    sparqlShowAll();
                } else {
                    sparqlSearch(query.getValue(), query2.getValue());
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

    String sparqlSearchResolver(String search) {
        String newSearch = "";
        if (search.contains("?") || search.contains("*")) {
            newSearch = SearchFunctions.searchToRegex(search);
        } else {
            newSearch = search;
        }

        if (newSearch.contains("<") && newSearch.contains(">")) {
            newSearch = SearchFunctions.addXWordContext(newSearch);
        }
        return newSearch;
    }

    String resolveSearchType(ComboBox<String> searchIn) {
        if (searchIn.getValue() == SearchInEnum.Meaning.name()) {
            return "?meaning";
        } else {
            return "?word";
        }
    }

    void sparqlSearch(String search, String secondSearch) {
        String newSearch = sparqlSearchResolver(search);
        String secondNewSearch = sparqlSearchResolver(secondSearch);

        String searchType = resolveSearchType(searchIn);
        String secondSearchType = StringUtils.isEmpty(searchIn2.getValue()) ? "?meaning" : resolveSearchType(searchIn2);

        String logicalOperand = LogicalOperatorEnum.AND.equals(logicalOperator.getValue()) ? "&&" : "||";

        FileManagerImpl.get().addLocatorClassLoader(MainView.class.getClassLoader());
        Model model = FileManagerImpl.get().loadModel(currentDirectory + "/dictionary.rdf");
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                " SELECT ?word ?meaning ?partOW WHERE { " +
                " ?x rdfs:label ?word . " +
                " ?x rdfs:comment ?meaning . " +
                " ?x rdfs:isDefinedBy ?partOW . " +
                " FILTER ("
                + (negateCheckBox.getValue() ? "!" : "") + "(regex(" + searchType + ", \"" + "^" + newSearch + "$\", \"i\") "
                + logicalOperand + " regex(" + secondSearchType + ", \"^" + secondNewSearch + "$\", \"i\"))) " +
                " } " +
                " LIMIT 10";
        Query query1 = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query1, model);
        try {
            System.out.println("1: " + qexec.getTimeout1());
            System.out.println("2: " + qexec.getTimeout2());
            ResultSet results = qexec.execSelect();
            System.out.println("reached here");
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("word");
                Literal meaning = soln.getLiteral("meaning");
                Literal partOW = soln.getLiteral("partOW");
                System.out.println(name);
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
        Model model = FileManagerImpl.get().loadModel(currentDirectory + "/dictionary.rdf");
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

    public void addItemsSearchIn(ComboBox<String> searchIn) {
        List<String> searchesIn = new ArrayList<>();
        searchesIn.add(SearchInEnum.Word.name());
        searchesIn.add(SearchInEnum.Meaning.name());
        searchIn.setItems(searchesIn);
    }

    public void addItemsLogicalOperator(ComboBox<LogicalOperatorEnum> logicalOperator) {
        List<LogicalOperatorEnum> logicalOperators = new ArrayList<>();
        logicalOperators.add(LogicalOperatorEnum.AND);
        logicalOperators.add(LogicalOperatorEnum.OR);
        logicalOperator.setItems(logicalOperators);
    }

}
