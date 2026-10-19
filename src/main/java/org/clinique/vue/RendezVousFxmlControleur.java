package org.clinique.vue;

import org.clinique.dao.MedecinDAO;
import org.clinique.dao.PatientDAO;
import org.clinique.dao.RendezVousDAO;
import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import org.clinique.modele.RendezVous;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RendezVousFxmlControleur {

    // Créneaux de 30 minutes : le matin de 9h à 12h, l'après-midi de 14h à 18h.
    private static final LocalTime DEBUT_MATIN = LocalTime.of(9, 0);
    private static final LocalTime FIN_MATIN = LocalTime.of(12, 0);
    private static final LocalTime DEBUT_APRES_MIDI = LocalTime.of(14, 0);
    private static final LocalTime FIN_APRES_MIDI = LocalTime.of(18, 0);
    private static final int DUREE_CRENEAU_MINUTES = 30;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private TableView<RendezVous> tableRendezVous;

    @FXML
    private TableColumn<RendezVous, LocalDate> colDate;
    @FXML
    private TableColumn<RendezVous, LocalTime> colHeure;
    @FXML
    private TableColumn<RendezVous, Medecin> colMedecin;
    @FXML
    private TableColumn<RendezVous, Patient> colPatient;

    @FXML
    private ComboBox<Medecin> comboMedecin;
    @FXML
    private ComboBox<Patient> comboPatient;
    @FXML
    private DatePicker champDate;
    @FXML
    private ComboBox<LocalTime> comboCreneau;

    @FXML
    private Label labelMessage;

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ObservableList<RendezVous> rendezVous = FXCollections.observableArrayList();
    private final ObservableList<Medecin> medecins = FXCollections.observableArrayList();
    private final ObservableList<Patient> patientsDuMedecin = FXCollections.observableArrayList();
    private final ObservableList<LocalTime> creneauxLibres = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colHeure.setCellValueFactory(new PropertyValueFactory<>("heure"));
        colMedecin.setCellValueFactory(new PropertyValueFactory<>("medecin"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patient"));

        // Affiche la date au format français (12/10/2026) plutôt que 2026-10-12.
        colDate.setCellFactory(colonne -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean vide) {
                super.updateItem(date, vide);
                setText(vide || date == null ? null : date.format(FORMAT_DATE));
            }
        });

        tableRendezVous.setItems(rendezVous);
        comboMedecin.setItems(medecins);
        comboPatient.setItems(patientsDuMedecin);
        comboCreneau.setItems(creneauxLibres);

        // Dans le calendrier, on grise les jours passés et le weekend.
        champDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean vide) {
                super.updateItem(date, vide);
                setDisable(vide || !jourOuvrable(date));
            }
        });

        // Choisir un médecin recharge la liste de SES patients et les créneaux libres.
        comboMedecin.valueProperty().addListener((observable, ancien, nouveau) -> {
            chargerPatientsDuMedecin(nouveau);
            rafraichirCreneaux();
        });

        // Changer de date recalcule les créneaux libres du médecin ce jour-là.
        champDate.valueProperty().addListener((observable, ancienne, nouvelle) -> rafraichirCreneaux());

        tableRendezVous.getSelectionModel().selectedItemProperty().addListener(
                (observable, ancienneSelection, nouvelleSelection) -> {
                    if (nouvelleSelection != null) {
                        remplirFormulaire(nouvelleSelection);
                    }
                });

        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            rendezVous.setAll(rendezVousDAO.trouverTous());
            medecins.setAll(medecinDAO.trouverTous());
            afficherMessage("");
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    @FXML
    private void onAjouter() {
        if (!formulaireValide(0)) {
            return;
        }

        try {
            RendezVous nouveau = new RendezVous(
                    champDate.getValue(),
                    comboCreneau.getValue(),
                    comboMedecin.getValue(),
                    comboPatient.getValue());
            rendezVousDAO.ajouter(nouveau);
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Rendez-vous ajouté.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        RendezVous selectionne = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un rendez-vous dans le tableau");
            return;
        }
        if (!formulaireValide(selectionne.getId())) {
            return;
        }

        try {
            selectionne.setMedecin(comboMedecin.getValue());
            selectionne.setPatient(comboPatient.getValue());
            selectionne.setDate(champDate.getValue());
            selectionne.setHeure(comboCreneau.getValue());

            rendezVousDAO.modifier(selectionne);
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Rendez-vous modifié.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        RendezVous selectionne = tableRendezVous.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un rendez-vous dans le tableau");
            return;
        }

        try {
            rendezVousDAO.supprimer(selectionne.getId());
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Rendez-vous supprimé.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onEffacer() {
        viderFormulaire();
        afficherMessage("");
    }

    @FXML
    private void onRafraichir() {
        chargerDonnees();
        viderFormulaire();
    }

    @FXML
    private void onVoirPatients() {
        changerDeVue("patient-vue.fxml", "Gestion des patients - BTS SIO SLAM");
    }

    @FXML
    private void onVoirMedecins() {
        changerDeVue("medecin-vue.fxml", "Gestion des médecins - BTS SIO SLAM");
    }

    @FXML
    private void onVoirInscription() {
        changerDeVue("inscription-vue.fxml", "Enregistrer un médecin / un patient - BTS SIO SLAM");
    }

    // --- Méthodes utilitaires privées : détails internes au contrôleur ---

    private void changerDeVue(String fichierFxml, String titre) {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource(fichierFxml));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) tableRendezVous.getScene().getWindow();
            fenetre.setTitle(titre);
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            afficherMessage("Impossible d'ouvrir la vue : " + e.getMessage());
        }
    }

    /**
     * Vérifie le formulaire avant un ajout ou une modification.
     * idIgnore = id du rendez-vous modifié (0 pour un ajout), pour ne pas
     * le compter comme un conflit avec lui-même.
     */
    private boolean formulaireValide(int idIgnore) {
        if (comboMedecin.getValue() == null || comboPatient.getValue() == null) {
            afficherMessage("Choisissez un médecin puis un de ses patients");
            return false;
        }
        if (champDate.getValue() == null || comboCreneau.getValue() == null) {
            afficherMessage("Choisissez une date et un créneau horaire");
            return false;
        }
        if (!jourOuvrable(champDate.getValue())) {
            afficherMessage("La date doit être un jour de semaine, aujourd'hui ou plus tard");
            return false;
        }
        // Le médecin ne peut pas être pris deux fois : la liste des créneaux ne
        // propose déjà que les créneaux libres. Reste à vérifier le patient,
        // qui pourrait avoir un rendez-vous avec un AUTRE médecin à la même heure.
        try {
            if (rendezVousDAO.patientDejaPris(comboPatient.getValue(), champDate.getValue(),
                    comboCreneau.getValue(), idIgnore)) {
                afficherMessage("Ce patient a déjà un rendez-vous sur ce créneau");
                return false;
            }
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean jourOuvrable(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        return !date.isBefore(LocalDate.now())
                && jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY;
    }

    private void chargerPatientsDuMedecin(Medecin medecin) {
        comboPatient.setValue(null);
        if (medecin == null) {
            patientsDuMedecin.clear();
            return;
        }
        try {
            patientsDuMedecin.setAll(patientDAO.trouverParMedecin(medecin));
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    /**
     * Recalcule la liste des créneaux proposés : tous les créneaux de la
     * journée, moins ceux déjà réservés pour ce médecin ce jour-là (sauf
     * celui du rendez-vous en cours de modification), moins ceux déjà
     * passés si la date est aujourd'hui.
     */
    private void rafraichirCreneaux() {
        LocalTime ancienChoix = comboCreneau.getValue();
        creneauxLibres.clear();

        Medecin medecin = comboMedecin.getValue();
        LocalDate date = champDate.getValue();
        if (medecin == null || date == null) {
            return;
        }

        RendezVous enModification = tableRendezVous.getSelectionModel().getSelectedItem();

        try {
            List<LocalTime> occupes = new ArrayList<>();
            for (RendezVous rdv : rendezVousDAO.trouverParMedecinEtDate(medecin, date)) {
                if (enModification == null || rdv.getId() != enModification.getId()) {
                    occupes.add(rdv.getHeure());
                }
            }

            for (LocalTime creneau : tousLesCreneaux()) {
                boolean dejaPasse = date.equals(LocalDate.now()) && creneau.isBefore(LocalTime.now());
                if (!occupes.contains(creneau) && !dejaPasse) {
                    creneauxLibres.add(creneau);
                }
            }
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }

        comboCreneau.setValue(creneauxLibres.contains(ancienChoix) ? ancienChoix : null);
    }

    private List<LocalTime> tousLesCreneaux() {
        List<LocalTime> creneaux = new ArrayList<>();
        for (LocalTime h = DEBUT_MATIN; h.isBefore(FIN_MATIN); h = h.plusMinutes(DUREE_CRENEAU_MINUTES)) {
            creneaux.add(h);
        }
        for (LocalTime h = DEBUT_APRES_MIDI; h.isBefore(FIN_APRES_MIDI); h = h.plusMinutes(DUREE_CRENEAU_MINUTES)) {
            creneaux.add(h);
        }
        return creneaux;
    }

    /**
     * Recopie le rendez-vous dans le formulaire. L'ordre compte : choisir le
     * médecin recharge la liste des patients, choisir la date recharge les
     * créneaux ; on sélectionne donc le patient et l'heure en dernier.
     */
    private void remplirFormulaire(RendezVous rdv) {
        comboMedecin.setValue(trouverMedecinDansListe(rdv.getMedecin()));
        comboPatient.setValue(trouverPatientDansListe(rdv.getPatient()));
        champDate.setValue(rdv.getDate());
        rafraichirCreneaux();
        // Un rendez-vous passé n'apparaît pas dans les créneaux libres : on l'ajoute pour l'afficher.
        if (!creneauxLibres.contains(rdv.getHeure())) {
            creneauxLibres.add(rdv.getHeure());
        }
        comboCreneau.setValue(rdv.getHeure());
    }

    /**
     * Même problème que dans PatientFxmlControleur : les objets du rendez-vous
     * et ceux des ComboBox viennent de requêtes différentes, on les retrouve par id.
     */
    private Medecin trouverMedecinDansListe(Medecin medecin) {
        for (Medecin m : medecins) {
            if (m.getId() == medecin.getId()) {
                return m;
            }
        }
        return null;
    }

    /**
     * Si le patient a changé de médecin traitant depuis la prise du rendez-vous,
     * il n'est plus dans la liste : on l'y ajoute pour pouvoir afficher le rendez-vous.
     */
    private Patient trouverPatientDansListe(Patient patient) {
        for (Patient p : patientsDuMedecin) {
            if (p.getId() == patient.getId()) {
                return p;
            }
        }
        patientsDuMedecin.add(patient);
        return patient;
    }

    private void viderFormulaire() {
        tableRendezVous.getSelectionModel().clearSelection();
        comboMedecin.setValue(null);
        champDate.setValue(null);
        comboCreneau.setValue(null);
    }

    private void afficherMessage(String message) {
        labelMessage.setText(message);
    }
}
