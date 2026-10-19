package org.clinique.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * MODELE : entité Hibernate représentant un médecin.
 *
 * C'est le côté "un" de la relation ManyToOne : un médecin peut être
 * le médecin traitant de plusieurs patients.
 */
@Entity
@Table(name = "medecin")
public class Medecin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // correspond à AUTO_INCREMENT côté MySQL
    @Column(name = "id")
    private int id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "specialite", nullable = false, length = 60)
    private String specialite;

    // Constructeur sans argument obligatoire pour Hibernate : il l'utilise
    // par réflexion pour recréer les objets à partir des lignes de la base.
    protected Medecin() {
    }

    public Medecin(String nom, String prenom, String specialite) {
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
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

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    @Override
    public String toString() {
        return "Dr " + prenom + " " + nom + " (" + specialite + ")";
    }
}
