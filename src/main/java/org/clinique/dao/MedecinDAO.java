package org.clinique.dao;

import org.clinique.modele.Medecin;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * DAO des médecins : mêmes méthodes CRUD que EtudiantDAO du premier projet
 * (trouverTous, trouverParId, ajouter, modifier, supprimer).
 */
public class MedecinDAO {

    /**
     * Récupère tous les médecins de la base.
     */
    public List<Medecin> trouverTous() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Medecin", Medecin.class).list();
        }
    }

    /**
     * Recherche un médecin par son identifiant.
     * Retourne null si aucun médecin ne correspond.
     */
    public Medecin trouverParId(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Medecin.class, id);
        }
    }

    /**
     * Insère un nouveau médecin en base et renvoie son id généré par MySQL.
     */
    public int ajouter(Medecin medecin) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(medecin);
            transaction.commit();
            return medecin.getId();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de l'ajout du médecin", e);
        }
    }

    /**
     * Met à jour les informations d'un médecin déjà existant (identifié par son id).
     */
    public void modifier(Medecin medecin) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(medecin);
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la modification du médecin id=" + medecin.getId(), e);
        }
    }

    /**
     * Supprime un médecin à partir de son id.
     *
     * Attention : MySQL refusera la suppression (clé étrangère fk_patient_medecin)
     * si des patients ont encore ce médecin comme médecin traitant. Le contrôleur
     * vérifie donc d'abord qu'aucun patient n'est rattaché à ce médecin.
     */
    public void supprimer(int id) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Medecin medecin = session.get(Medecin.class, id);
            if (medecin != null) {
                session.remove(medecin);
            }
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la suppression du médecin id=" + id, e);
        }
    }
}
