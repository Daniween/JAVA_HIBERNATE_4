-- Script de création de la base "clinique_db" (BTS SIO SLAM) - identique au projet 3.
-- À exécuter AVANT de lancer l'application (dans un client MySQL : MySQL Workbench,
-- phpMyAdmin, DBeaver, ou en ligne de commande : mysql -u root -p < clinique_db.sql)

CREATE DATABASE IF NOT EXISTS clinique_db CHARACTER SET utf8mb4;
USE clinique_db;

-- Côté "un" de la relation
CREATE TABLE medecin (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(50) NOT NULL,
    prenom      VARCHAR(50) NOT NULL,
    specialite  VARCHAR(60) NOT NULL
);

-- Côté "plusieurs" de la relation : medecin_id est la clé étrangère
-- qui correspond à l'annotation @ManyToOne + @JoinColumn(name = "medecin_id")
CREATE TABLE patient (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(50) NOT NULL,
    prenom      VARCHAR(50) NOT NULL,
    telephone   VARCHAR(20),
    medecin_id  INT NOT NULL,
    CONSTRAINT fk_patient_medecin FOREIGN KEY (medecin_id) REFERENCES medecin(id)
);

-- Table des rendez-vous : elle porte DEUX clés étrangères (deux relations ManyToOne).
-- Un rendez-vous = un médecin + un patient + une date + un créneau horaire.
-- Les contraintes UNIQUE empêchent deux rendez-vous sur le même créneau
-- pour un même médecin, ou pour un même patient.
CREATE TABLE rendez_vous (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    date_rdv    DATE NOT NULL,
    heure       TIME NOT NULL,
    medecin_id  INT NOT NULL,
    patient_id  INT NOT NULL,
    CONSTRAINT fk_rdv_medecin FOREIGN KEY (medecin_id) REFERENCES medecin(id),
    CONSTRAINT fk_rdv_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT uq_rdv_medecin_creneau UNIQUE (medecin_id, date_rdv, heure),
    CONSTRAINT uq_rdv_patient_creneau UNIQUE (patient_id, date_rdv, heure)
);

-- Données d'exemple : 3 médecins, 6 patients répartis entre eux
INSERT INTO medecin (nom, prenom, specialite) VALUES
    ('Lefevre', 'Claire', 'Généraliste'),
    ('Morel', 'Julien', 'Cardiologue'),
    ('Girard', 'Sophie', 'Pédiatre');

INSERT INTO patient (nom, prenom, telephone, medecin_id) VALUES
    ('Martin', 'Lucas', '0601020304', 1),
    ('Bernard', 'Emma', '0602030405', 1),
    ('Dubois', 'Nathan', '0603040506', 2),
    ('Thomas', 'Chloe', '0604050607', 2),
    ('Robert', 'Lea', '0605060708', 3),
    ('Petit', 'Hugo', '0606070809', 3);

INSERT INTO rendez_vous (date_rdv, heure, medecin_id, patient_id) VALUES
    ('2026-10-12', '09:00:00', 1, 1),
    ('2026-10-12', '09:30:00', 1, 2),
    ('2026-10-13', '14:00:00', 2, 3),
    ('2026-10-14', '10:30:00', 3, 5);

-- Un utilisateur applicatif dédié (à adapter/retirer selon votre configuration MySQL) :
-- CREATE USER 'user'@'localhost' IDENTIFIED BY 'userpassword';
-- GRANT ALL PRIVILEGES ON clinique_db.* TO 'user'@'localhost';
-- FLUSH PRIVILEGES;
