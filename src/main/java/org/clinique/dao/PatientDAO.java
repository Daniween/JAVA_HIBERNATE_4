package org.clinique.dao;

import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * DAO = Data Access Object.
 *
 * Aucun SQL écrit à la main : on manipule des objets Patient via une
 * Session Hibernate. Comme Patient.medecin est en fetch EAGER, Hibernate
 * charge le médecin associé dans la même requête (jointure SQL) : on peut
 * donc appeler patient.getMedecin() même après la fermeture de la Session.
 *
 * Pour l'écriture (ajouter/modifier), il suffit que l'attribut "medecin"
 * du patient contienne un Medecin existant : Hibernate écrit tout seul
 * son id dans la colonne de clé étrangère medecin_id.
 */
public class PatientDAO {

    /**
     * Récupère tous les patients de la base, avec leur médecin traitant déjà chargé.
     */
    public List<Patient> trouverTous() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Patient", Patient.class).list();
        }
    }

    /**
     * Récupère tous les patients suivis par un médecin donné.
     *
     * La relation reste unidirectionnelle (Patient → Medecin) : on n'a pas
     * besoin d'une liste de patients dans Medecin, il suffit de filtrer les
     * patients sur leur attribut "medecin" en HQL. Hibernate traduit
     * "p.medecin = :medecin" en "WHERE medecin_id = ?".
     */
    public List<Patient> trouverParMedecin(Medecin medecin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Patient p where p.medecin = :medecin", Patient.class)
                    .setParameter("medecin", medecin)
                    .list();
        }
    }

    /**
     * Recherche un patient par son identifiant.
     * Retourne null si aucun patient ne correspond.
     */
    public Patient trouverParId(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Patient.class, id);
        }
    }

    /**
     * Insère un nouveau patient en base et renvoie son id généré par MySQL.
     * Hibernate génère : INSERT INTO patient (nom, prenom, telephone, medecin_id) VALUES (?, ?, ?, ?)
     */
    public int ajouter(Patient patient) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(patient);
            transaction.commit();
            return patient.getId();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de l'ajout du patient", e);
        }
    }

    /**
     * Met à jour un patient existant, y compris un éventuel changement de médecin traitant.
     */
    public void modifier(Patient patient) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(patient);
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la modification du patient id=" + patient.getId(), e);
        }
    }

    /**
     * Supprime un patient à partir de son id (le médecin, lui, n'est pas touché).
     */
    public void supprimer(int id) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Patient patient = session.get(Patient.class, id);
            if (patient != null) {
                session.remove(patient);
            }
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la suppression du patient id=" + id, e);
        }
    }
}
