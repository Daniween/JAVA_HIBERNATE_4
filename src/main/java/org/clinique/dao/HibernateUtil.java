package org.clinique.dao;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = construireSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory construireSessionFactory() {
        try {
            // configure() lit automatiquement src/main/resources/hibernate.cfg.xml
            return new Configuration()
                    .configure()
                    .buildSessionFactory();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Erreur d'initialisation de Hibernate : " + e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }
}
