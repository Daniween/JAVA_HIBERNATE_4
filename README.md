# Tutoriel : formulaire d'enregistrement d'un médecin et d'un patient (Hibernate + JavaFX + MySQL)

Objectif : partir du projet `3_JAVA_Hibernate_CRUD_ManyToOne` et y ajouter une
**vue d'enregistrement** avec deux formulaires : **Nouveau médecin** et
**Nouveau patient**.

Le contrôleur fait toujours la même chose :

1. **lire** les champs du formulaire ;
2. **créer l'objet** (`new Medecin(...)` ou `new Patient(...)`) ;
3. **appeler le DAO** (`medecinDAO.ajouter(...)`), qui l'enregistre en base avec Hibernate.

Prérequis : identiques aux projets précédents (IntelliJ, JDK 17+, MySQL,
SceneBuilder).

---

## Étape 1 - La base de données

**Aucun changement** : même script que le projet 3.

```
mysql -u root -p < database/clinique_db.sql
```

Les contraintes à respecter dans le formulaire :

| Table     | Colonne      | Contrainte                    | Dans le formulaire                   |
| --------- | ------------ | ----------------------------- | ------------------------------------ |
| `medecin` | `nom`        | `NOT NULL`                    | champ obligatoire `*`                |
| `medecin` | `prenom`     | `NOT NULL`                    | champ obligatoire `*`                |
| `medecin` | `specialite` | `NOT NULL`                    | champ obligatoire `*`                |
| `patient` | `nom`        | `NOT NULL`                    | champ obligatoire `*`                |
| `patient` | `prenom`     | `NOT NULL`                    | champ obligatoire `*`                |
| `patient` | `telephone`  | facultatif                    | champ facultatif                     |
| `patient` | `medecin_id` | `NOT NULL` + clé étrangère    | `ComboBox` : choisir un médecin `*`  |

⚠️ Adapter `hibernate.cfg.xml` (URL de la base, `username`, `password`) à
votre configuration MySQL.

---

## Étape 2 - Créer le projet

Copier le projet 3. Dans le `pom.xml`, seul l'`artifactId` change :

```xml
<artifactId>ORMFormulaireContraintes</artifactId>
```

Les entités, les DAO, `HibernateUtil.java` et `hibernate.cfg.xml` sont
**inchangés**.

---

## Étape 3 - La vue : `inscription-vue.fxml`

Deux formulaires côte à côte (`HBox`), chacun dans une `VBox` :

```xml
<GridPane hgap="10.0" vgap="8.0">
    <Label text="Nom * :" GridPane.rowIndex="0" GridPane.columnIndex="0"/>
    <TextField fx:id="champMedecinNom" GridPane.rowIndex="0" GridPane.columnIndex="1"/>
    ...
</GridPane>

<Button text="Enregistrer le médecin" onAction="#onEnregistrerMedecin"/>
<Label fx:id="messageMedecin" textFill="#b00020"/>
```

Pour le patient, le médecin traitant se choisit dans une `ComboBox`.
C'est la clé étrangère `medecin_id` vue côté interface :

```xml
<ComboBox fx:id="comboPatientMedecin" prefWidth="250.0" promptText="Choisir un médecin"
          GridPane.rowIndex="3" GridPane.columnIndex="1"/>
```

---

## Étape 4 - Le contrôleur : `InscriptionFxmlControleur.java`

### Le chemin du code quand on clique sur "Enregistrer le médecin"

```
inscription-vue.fxml      onAction="#onEnregistrerMedecin"
        ▼
InscriptionFxmlControleur.onEnregistrerMedecin()
        │  1. vérifie que les champs obligatoires sont remplis
        │  2. new Medecin(nom, prenom, specialite)
        ▼
MedecinDAO.ajouter(medecin)     Session + Transaction → persist → commit
        ▼
Hibernate → INSERT INTO medecin (nom, prenom, specialite) VALUES (?, ?, ?)
```

```java
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

        // 2. Le DAO l'enregistre
        int id = medecinDAO.ajouter(nouveau);

        messageMedecin.setText("Médecin enregistré (id " + id + ").");
        chargerMedecins();   // le nouveau médecin apparaît dans la ComboBox du patient
    } catch (RuntimeException e) {
        messageMedecin.setText("Erreur : " + e.getMessage());
    }
}
```

### Enregistrer un patient

Même principe, avec en plus la vérification de la `ComboBox` :

```java
if (comboPatientMedecin.getValue() == null) {
    messagePatient.setText("Choisissez un médecin traitant");
    return;
}

Patient nouveau = new Patient(
        champPatientNom.getText().trim(),
        champPatientPrenom.getText().trim(),
        champPatientTelephone.getText().trim(),
        comboPatientMedecin.getValue());   // l'objet Medecin choisi
patientDAO.ajouter(nouveau);               // Hibernate écrit son id dans medecin_id
```

---

## Étape 5 - Navigation

- `PatientApplication` démarre maintenant sur `inscription-vue.fxml`.
- Les vues de gestion du projet 3 (patients, médecins, rendez-vous) ont un
  bouton **Enregistrer médecin / patient** pour revenir au formulaire
  (méthode `onVoirInscription()` dans chaque contrôleur).

### Un rôle par vue : pas de doublon pour l'ajout

Dans le projet 3, les vues de gestion avaient un bouton **Ajouter**. En gardant
aussi la vue d'enregistrement, on aurait **deux chemins** pour créer un médecin
(`MedecinFxmlControleur.onAjouter()` et `InscriptionFxmlControleur.onEnregistrerMedecin()`),
donc du code en double à maintenir.

On sépare donc les rôles :

| Vue                                     | Contrôleur                                       | Rôle                                   |
| --------------------------------------- | ------------------------------------------------ | -------------------------------------- |
| `inscription-vue.fxml`                  | `InscriptionFxmlControleur`                      | **Créer** un médecin ou un patient     |
| `medecin-vue.fxml` / `patient-vue.fxml` | `MedecinFxmlControleur` / `PatientFxmlControleur` | **Consulter, modifier, supprimer**     |

Le bouton **Ajouter** et la méthode `onAjouter()` sont **supprimés** des vues
des médecins et des patients. Le seul appel à `medecinDAO.ajouter(...)` (et à
`patientDAO.ajouter(...)`) est dans `InscriptionFxmlControleur`.

> La vue des rendez-vous garde son bouton **Ajouter** : la création d'un
> rendez-vous n'existe qu'à cet endroit.

---

## Étape 6 - Arborescence

```
4_JAVA_Hibernate_Formulaire_Contraintes/
├── database/
│   └── clinique_db.sql                    (identique au projet 3)
├── pom.xml
└── src/main/
    ├── java/org/clinique/
    │   ├── PatientApplication.java        (démarre sur inscription-vue.fxml)
    │   ├── modele/                        (inchangé)
    │   ├── dao/                           (inchangé)
    │   └── vue/
    │       ├── InscriptionFxmlControleur.java   ← NOUVEAU
    │       ├── MedecinFxmlControleur.java       (- onAjouter, + onVoirInscription)
    │       ├── PatientFxmlControleur.java       (- onAjouter, + onVoirInscription)
    │       └── RendezVousFxmlControleur.java    (+ onVoirInscription)
    └── resources/
        ├── hibernate.cfg.xml
        └── org/clinique/vue/
            ├── inscription-vue.fxml       ← NOUVEAU
            ├── medecin-vue.fxml
            ├── patient-vue.fxml
            └── rendezvous-vue.fxml
```

---

## Étape 7 - Lancer et tester

1. Exécuter `database/clinique_db.sql` si ce n'est pas déjà fait.
2. Lancer `PatientApplication` depuis IntelliJ, ou `mvn javafx:run`.

Scénario de test :

1. **Enregistrer le médecin** sans rien remplir → message "obligatoires".
2. Remplir nom, prénom, spécialité → "Médecin enregistré (id 4)".
3. Le nouveau médecin est disponible dans la liste du formulaire patient.
4. **Enregistrer le patient** sans choisir de médecin → "Choisissez un médecin traitant".
5. Remplir et choisir un médecin → "Patient enregistré".
6. **Gérer les patients** → le nouveau patient est dans le tableau avec son médecin.
   Il n'y a plus de bouton Ajouter : on peut le modifier ou le supprimer.

---

## Pour plus tard

Améliorations prévues dans une prochaine étape :

- limiter la saisie à la taille des colonnes `VARCHAR(50)` et `VARCHAR(60)` ;
- afficher l'erreur sous le champ concerné ;
- vérifier le format du téléphone ;
- enregistrer `NULL` plutôt que `''` quand le téléphone est vide ;
- regrouper ces règles dans des classes de validation.

---

## Dépannage des erreurs fréquentes

| Symptôme                                                              | Cause probable                                            | Solution                                                       |
| ---------------------------------------------------------------------- | --------------------------------------------------------- | -------------------------------------------------------------- |
| La `ComboBox` des médecins est vide                                    | Aucun médecin en base, ou base inaccessible                | Enregistrer d'abord un médecin ; vérifier `hibernate.cfg.xml`  |
| `Data too long for column 'nom'`                                       | Texte plus long que le `VARCHAR` de la colonne             | Saisir un texte plus court                                     |
| `HibernateException: Unable to open JDBC Connection` / `Access denied` | Mauvais identifiants, ou MySQL non démarré                 | Vérifier l'URL, le `username`/`password`, le service MySQL     |
| `Location is not set` au chargement du FXML                           | Le `.fxml` n'est pas dans `src/main/resources/org/clinique/vue/` | Vérifier l'emplacement des fichiers FXML                 |
