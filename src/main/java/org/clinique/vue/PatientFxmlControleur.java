package org.clinique.vue;

import org.clinique.dao.MedecinDAO;
import org.clinique.dao.PatientDAO;
import org.clinique.dao.RendezVousDAO;
import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Gestion des patients : consulter, modifier, supprimer.
 * La CRÉATION d'un patient se fait uniquement dans la vue d'enregistrement
 * (InscriptionFxmlControleur) : un seul chemin pour patientDAO.ajouter(...).
 */
public class PatientFxmlControleur {

    @FXML
    private TableView<Patient> tablePatients;

    @FXML
    private TableColumn<Patient, Integer> colId;
    @FXML
    private TableColumn<Patient, String> colNom;
    @FXML
    private TableColumn<Patient, String> colPrenom;
    @FXML
    private TableColumn<Patient, String> colTelephone;
    @FXML
    private TableColumn<Patient, Medecin> colMedecin;

    // --- Formulaire de saisie ---

    @FXML
    private TextField champNom;
    @FXML
    private TextField champPrenom;
    @FXML
    private TextField champTelephone;
    @FXML
    private ComboBox<Medecin> comboMedecin;

    // --- Panneau d'informations sur le médecin traitant ---

    @FXML
    private Label labelMedecinNom;
    @FXML
    private Label labelMedecinPrenom;
    @FXML
    private Label labelMedecinSpecialite;

    @FXML
    private Label labelMessage;

    // --- Accès aux données : le contrôleur s'appuie sur les DAO, jamais sur du SQL direct ---

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();

    // Liste "observable" : la TableView se redessine automatiquement
    // quand son contenu change (contrairement à une simple List Java).
    private final ObservableList<Patient> donnees = FXCollections.observableArrayList();
    private final ObservableList<Medecin> medecins = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        // La colonne affiche patient.getMedecin(), donc le toString() de Medecin : "Dr Prénom Nom (Spécialité)"
        colMedecin.setCellValueFactory(new PropertyValueFactory<>("medecin"));

        tablePatients.setItems(donnees);
        comboMedecin.setItems(medecins);

        // Au clic sur une ligne : on recopie le patient dans le formulaire (pour le modifier)
        // et on affiche son médecin traitant dans le panneau du bas.
        tablePatients.getSelectionModel().selectedItemProperty().addListener(
                (observable, ancienneSelection, nouvelleSelection) -> {
                    if (nouvelleSelection != null) {
                        remplirFormulaire(nouvelleSelection);
                        afficherMedecin(nouvelleSelection.getMedecin());
                    } else {
                        viderMedecin();
                    }
                });

        chargerDonnees();
    }

    /**
     * Recharge les patients (tableau) et les médecins (liste déroulante) depuis la base.
     */
    private void chargerDonnees() {
        try {
            donnees.setAll(patientDAO.trouverTous());
            medecins.setAll(medecinDAO.trouverTous());
            afficherMessage("");
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        Patient selectionne = tablePatients.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un patient dans le tableau");
            return;
        }
        if (!formulaireValide()) {
            return;
        }

        try {
            selectionne.setNom(champNom.getText().trim());
            selectionne.setPrenom(champPrenom.getText().trim());
            selectionne.setTelephone(champTelephone.getText().trim());
            // Changer de médecin traitant = simplement changer l'objet référencé :
            // Hibernate mettra à jour la colonne medecin_id tout seul.
            selectionne.setMedecin(comboMedecin.getValue());

            patientDAO.modifier(selectionne);
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Patient modifié.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        Patient selectionne = tablePatients.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un patient dans le tableau");
            return;
        }

        try {
            // Contrainte de clé étrangère fk_rdv_patient : un patient qui a encore
            // des rendez-vous ne peut pas être supprimé.
            int nbRendezVous = rendezVousDAO.trouverParPatient(selectionne).size();
            if (nbRendezVous > 0) {
                afficherMessage("Impossible : ce patient a encore " + nbRendezVous
                        + " rendez-vous. Supprimez-les d'abord dans la gestion des rendez-vous.");
                return;
            }

            patientDAO.supprimer(selectionne.getId());
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Patient supprimé.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onEffacer() {
        tablePatients.getSelectionModel().clearSelection();
        viderFormulaire();
        afficherMessage("");
    }

    @FXML
    private void onRafraichir() {
        chargerDonnees();
        viderFormulaire();
    }

    // Remplace le contenu de la fenêtre actuelle par la vue "Gestion des médecins".
    @FXML
    private void onVoirMedecins() {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource("medecin-vue.fxml"));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) tablePatients.getScene().getWindow();
            fenetre.setTitle("Gestion des médecins - BTS SIO SLAM");
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            afficherMessage("Impossible d'ouvrir la vue des médecins : " + e.getMessage());
        }
    }

    @FXML
    private void onVoirRendezVous() {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource("rendezvous-vue.fxml"));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) tablePatients.getScene().getWindow();
            fenetre.setTitle("Gestion des rendez-vous - BTS SIO SLAM");
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            afficherMessage("Impossible d'ouvrir la vue des rendez-vous : " + e.getMessage());
        }
    }

    // Ouvre la vue "Enregistrer un médecin / un patient" dans la même fenêtre.
    @FXML
    private void onVoirInscription() {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource("inscription-vue.fxml"));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) tablePatients.getScene().getWindow();
            fenetre.setTitle("Enregistrer un médecin / un patient - BTS SIO SLAM");
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            afficherMessage("Impossible d'ouvrir la vue d'enregistrement : " + e.getMessage());
        }
    }

    // --- Méthodes utilitaires privées : détails internes au contrôleur ---

    private boolean formulaireValide() {
        if (champNom.getText().isBlank() || champPrenom.getText().isBlank()) {
            afficherMessage("Le nom et le prénom sont obligatoires");
            return false;
        }
        if (comboMedecin.getValue() == null) {
            afficherMessage("Choisissez un médecin traitant");
            return false;
        }
        return true;
    }

    private void remplirFormulaire(Patient patient) {
        champNom.setText(patient.getNom());
        champPrenom.setText(patient.getPrenom());
        champTelephone.setText(patient.getTelephone());
        comboMedecin.setValue(trouverMedecinDansListe(patient.getMedecin()));
    }

    /**
     * Le médecin du patient et ceux de la ComboBox sont des objets Java différents
     * (chargés par deux requêtes distinctes) : on retrouve donc celui de la liste
     * qui a le même id, pour que la ComboBox le sélectionne correctement.
     */
    private Medecin trouverMedecinDansListe(Medecin medecin) {
        for (Medecin m : medecins) {
            if (m.getId() == medecin.getId()) {
                return m;
            }
        }
        return null;
    }

    private void viderFormulaire() {
        champNom.clear();
        champPrenom.clear();
        champTelephone.clear();
        comboMedecin.setValue(null);
        viderMedecin();
    }

    private void afficherMedecin(Medecin medecin) {
        labelMedecinNom.setText(medecin.getNom());
        labelMedecinPrenom.setText(medecin.getPrenom());
        labelMedecinSpecialite.setText(medecin.getSpecialite());
    }

    private void viderMedecin() {
        labelMedecinNom.setText("");
        labelMedecinPrenom.setText("");
        labelMedecinSpecialite.setText("");
    }

    private void afficherMessage(String message) {
        labelMessage.setText(message);
    }
}
