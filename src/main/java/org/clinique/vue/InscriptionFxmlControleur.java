package org.clinique.vue;

import org.clinique.dao.MedecinDAO;
import org.clinique.dao.PatientDAO;
import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur de la vue d'enregistrement.
 *
 * Toujours le même principe :
 * 1. lire les champs du formulaire ;
 * 2. créer l'objet (Medecin ou Patient) ;
 * 3. appeler le DAO, qui l'enregistre en base avec Hibernate.
 */
public class InscriptionFxmlControleur {

    // --- Formulaire médecin ---

    @FXML
    private TextField champMedecinNom;
    @FXML
    private TextField champMedecinPrenom;
    @FXML
    private TextField champMedecinSpecialite;
    @FXML
    private Label messageMedecin;

    // --- Formulaire patient ---

    @FXML
    private TextField champPatientNom;
    @FXML
    private TextField champPatientPrenom;
    @FXML
    private TextField champPatientTelephone;
    @FXML
    private ComboBox<Medecin> comboPatientMedecin;
    @FXML
    private Label messagePatient;

    // --- Accès aux données : le contrôleur s'appuie sur les DAO, jamais sur du SQL direct ---

    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ObservableList<Medecin> medecins = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        comboPatientMedecin.setItems(medecins);
        chargerMedecins();
    }

    private void chargerMedecins() {
        try {
            medecins.setAll(medecinDAO.trouverTous());
        } catch (RuntimeException e) {
            messagePatient.setText("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    @FXML
    private void onEnregistrerMedecin() {
        // Les trois colonnes sont NOT NULL dans la table medecin
        if (champMedecinNom.getText().isBlank() || champMedecinPrenom.getText().isBlank()
                || champMedecinSpecialite.getText().isBlank()) {
            messageMedecin.setText("Le nom, le prénom et la spécialité sont obligatoires");
            return;
        }

        try {
            // 1. On crée l'objet à partir des champs du formulaire
            Medecin nouveau = new Medecin(
                    champMedecinNom.getText().trim(),
                    champMedecinPrenom.getText().trim(),
                    champMedecinSpecialite.getText().trim());

            // 2. Le DAO l'enregistre : INSERT INTO medecin (nom, prenom, specialite) VALUES (?, ?, ?)
            int id = medecinDAO.ajouter(nouveau);

            champMedecinNom.clear();
            champMedecinPrenom.clear();
            champMedecinSpecialite.clear();
            messageMedecin.setText("Médecin enregistré (id " + id + ").");

            // Le nouveau médecin apparaît tout de suite dans la liste du formulaire patient
            chargerMedecins();
        } catch (RuntimeException e) {
            messageMedecin.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onEnregistrerPatient() {
        // nom et prenom sont NOT NULL ; medecin_id est NOT NULL (clé étrangère)
        if (champPatientNom.getText().isBlank() || champPatientPrenom.getText().isBlank()) {
            messagePatient.setText("Le nom et le prénom sont obligatoires");
            return;
        }
        if (comboPatientMedecin.getValue() == null) {
            messagePatient.setText("Choisissez un médecin traitant");
            return;
        }

        try {
            // 1. On crée l'objet : le médecin choisi dans la ComboBox est un objet Medecin
            Patient nouveau = new Patient(
                    champPatientNom.getText().trim(),
                    champPatientPrenom.getText().trim(),
                    champPatientTelephone.getText().trim(),
                    comboPatientMedecin.getValue());

            // 2. Le DAO l'enregistre : Hibernate écrit l'id du médecin dans medecin_id
            int id = patientDAO.ajouter(nouveau);

            champPatientNom.clear();
            champPatientPrenom.clear();
            champPatientTelephone.clear();
            comboPatientMedecin.setValue(null);
            messagePatient.setText("Patient enregistré (id " + id + ").");
        } catch (RuntimeException e) {
            messagePatient.setText("Erreur : " + e.getMessage());
        }
    }

    // --- Navigation : remplace le contenu de la fenêtre par une autre vue ---

    @FXML
    private void onVoirPatients() {
        changerDeVue("patient-vue.fxml", "Gestion des patients - BTS SIO SLAM");
    }

    @FXML
    private void onVoirMedecins() {
        changerDeVue("medecin-vue.fxml", "Gestion des médecins - BTS SIO SLAM");
    }

    @FXML
    private void onVoirRendezVous() {
        changerDeVue("rendezvous-vue.fxml", "Gestion des rendez-vous - BTS SIO SLAM");
    }

    private void changerDeVue(String fichierFxml, String titre) {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource(fichierFxml));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) champMedecinNom.getScene().getWindow();
            fenetre.setTitle(titre);
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            messageMedecin.setText("Impossible d'ouvrir la vue : " + e.getMessage());
        }
    }
}
