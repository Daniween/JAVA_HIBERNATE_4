package org.clinique.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * MODELE : entité Hibernate représentant un patient.
 *
 * C'est le côté "plusieurs" de la relation : chaque patient a un seul
 * médecin traitant, mais un même médecin peut suivre plusieurs patients.
 *
 * @ManyToOne + @JoinColumn indiquent à Hibernate que la table "patient"
 * porte une colonne "medecin_id" qui référence la clé primaire de la
 * table "medecin" (clé étrangère).
 */
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "telephone", length = 20)
    private String telephone;

    // fetch = EAGER : le médecin est chargé en même temps que le patient
    // (une seule requête, avec jointure), pour pouvoir l'afficher même
    // après la fermeture de la Session. C'est déjà le comportement par
    // défaut de @ManyToOne, il est précisé ici pour que ce soit explicite.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    protected Patient() {
    }

    public Patient(String nom, String prenom, String telephone, Medecin medecin) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.medecin = medecin;
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Medecin getMedecin() {
        return medecin;
    }

    public void setMedecin(Medecin medecin) {
        this.medecin = medecin;
    }

    @Override
    public String toString() {
        return "#" + id + " " + prenom + " " + nom;
    }
}
