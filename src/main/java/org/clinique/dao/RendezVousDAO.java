package org.clinique.dao;

import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import org.clinique.modele.RendezVous;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO des rendez-vous : mêmes méthodes CRUD que les autres DAO, plus des
 * recherches utiles pour savoir quels créneaux sont déjà pris.
 */
public class RendezVousDAO {

    /**
     * Récupère tous les rendez-vous, triés par date puis par heure.
     */
    public List<RendezVous> trouverTous() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from RendezVous r order by r.date, r.heure", RendezVous.class).list();
        }
    }

    /**
     * Récupère les rendez-vous d'un médecin pour un jour donné
     * (sert à calculer les créneaux encore libres).
     */
    public List<RendezVous> trouverParMedecinEtDate(Medecin medecin, LocalDate date) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from RendezVous r where r.medecin = :medecin and r.date = :date order by r.heure",
                            RendezVous.class)
                    .setParameter("medecin", medecin)
                    .setParameter("date", date)
                    .list();
        }
    }

    /**
     * Récupère tous les rendez-vous d'un médecin.
     */
    public List<RendezVous> trouverParMedecin(Medecin medecin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from RendezVous r where r.medecin = :medecin", RendezVous.class)
                    .setParameter("medecin", medecin)
                    .list();
        }
    }

    /**
     * Récupère tous les rendez-vous d'un patient.
     */
    public List<RendezVous> trouverParPatient(Patient patient) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from RendezVous r where r.patient = :patient", RendezVous.class)
                    .setParameter("patient", patient)
                    .list();
        }
    }

    /**
     * Indique si le patient a déjà un rendez-vous (avec n'importe quel médecin)
     * sur ce créneau. idIgnore permet d'exclure le rendez-vous en cours de
     * modification (mettre 0 lors d'un ajout).
     */
    public boolean patientDejaPris(Patient patient, LocalDate date, LocalTime heure, int idIgnore) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long nombre = session.createQuery(
                            "select count(r) from RendezVous r where r.patient = :patient"
                                    + " and r.date = :date and r.heure = :heure and r.id <> :idIgnore",
                            Long.class)
                    .setParameter("patient", patient)
                    .setParameter("date", date)
                    .setParameter("heure", heure)
                    .setParameter("idIgnore", idIgnore)
                    .uniqueResult();
            return nombre > 0;
        }
    }

    public RendezVous trouverParId(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(RendezVous.class, id);
        }
    }

    /**
     * Insère un nouveau rendez-vous et renvoie son id généré par MySQL.
     * Hibernate génère : INSERT INTO rendez_vous (date_rdv, heure, medecin_id, patient_id) VALUES (?, ?, ?, ?)
     */
    public int ajouter(RendezVous rendezVous) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(rendezVous);
            transaction.commit();
            return rendezVous.getId();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de l'ajout du rendez-vous", e);
        }
    }

    /**
     * Met à jour un rendez-vous existant (date, créneau, médecin ou patient).
     */
    public void modifier(RendezVous rendezVous) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(rendezVous);
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la modification du rendez-vous id=" + rendezVous.getId(), e);
        }
    }

    public void supprimer(int id) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            RendezVous rendezVous = session.get(RendezVous.class, id);
            if (rendezVous != null) {
                session.remove(rendezVous);
            }
            transaction.commit();

        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Erreur lors de la suppression du rendez-vous id=" + id, e);
        }
    }
}
