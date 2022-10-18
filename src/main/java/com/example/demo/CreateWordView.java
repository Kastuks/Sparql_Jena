package com.example.demo;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;

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

@Route("/create")
public class CreateWordView extends VerticalLayout {

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

    public CreateWordView(SparqlDataService service) {
        this.sparqlDataService = service;
        add(getDataForm());

    }
    private Component getDataForm() {

        var layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);

        var addButton = new Button("Add to database");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var redirectButton = new Button("Back to main page");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(word, meaning, addButton, redirectButton);

        binder.bindInstanceFields(this);

        addButton.addClickListener(click -> {
            try {
                //todo Add words to rdf files
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
