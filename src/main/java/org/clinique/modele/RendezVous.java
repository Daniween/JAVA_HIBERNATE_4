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

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * MODELE : entité Hibernate représentant un rendez-vous.
 *
 * Un rendez-vous porte DEUX relations ManyToOne :
 * - un médecin peut avoir plusieurs rendez-vous (colonne medecin_id) ;
 * - un patient peut avoir plusieurs rendez-vous (colonne patient_id).
 *
 * La date et l'heure sont stockées séparément : Hibernate fait
 * correspondre LocalDate au type SQL DATE et LocalTime au type SQL TIME.
 */
@Entity
@Table(name = "rendez_vous")
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "date_rdv", nullable = false)
    private LocalDate date;

    @Column(name = "heure", nullable = false)
    private LocalTime heure;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    // Le patient est lui-même chargé avec son médecin traitant (EAGER dans Patient).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    protected RendezVous() {
    }

    public RendezVous(LocalDate date, LocalTime heure, Medecin medecin, Patient patient) {
        this.date = date;
        this.heure = heure;
        this.medecin = medecin;
        this.patient = patient;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public void setHeure(LocalTime heure) {
        this.heure = heure;
    }

    public Medecin getMedecin() {
        return medecin;
    }

    public void setMedecin(Medecin medecin) {
        this.medecin = medecin;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public String toString() {
        return date + " " + heure + " : " + patient + " avec " + medecin;
    }
}
