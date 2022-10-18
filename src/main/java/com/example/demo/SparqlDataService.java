package com.example.demo;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SparqlDataService {

    @PersistenceContext
    private EntityManager em;


    private final SparqlDataRepository sparqlDataRepository;

    public SparqlDataService(SparqlDataRepository sparqlDataRepository) {
        this.sparqlDataRepository = sparqlDataRepository;
    }


    public void addData(SparqlData data) {
        sparqlDataRepository.save(data);
    }

    public List<SparqlData> findAllData() {

        return sparqlDataRepository.findAll();
    }
    public void deleteAll() {
        sparqlDataRepository.deleteAll();
    }
}
