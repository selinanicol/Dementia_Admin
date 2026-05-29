# Dementia – Admin- & Angehörigen-App

> **Status:** Prototyp (Iteration 2 / Beta)  
> **Zielgruppe:** Angehörige, Betreuer und Pflegekräfte  
> **Zweck:** Fernverwaltung des Patienten-Clients und Echtzeit-Eskalationsmanagement  

---

##  rojektbeschreibung

Diese Anwendung bildet das administrative Steuerzentrum des Systems. Sie ermöglicht es dem Angehörigen, den Medikamentenplan und wichtige Kontakte aus der Ferne (Remote) komplett zu verwalten. Gleichzeitig fungiert sie als Überwachungs-Monitor, der im Ernstfall sofort alarmiert, wenn der Patient eine Einnahme versäumt hat.

---

## Kernfunktionen

* **Remote-Planer:** Erstellen und Editieren von Medikamenten mit flexiblen Intervallen (Einmalig mit Datum, Täglich oder Wöchentlich nach Wochentag).
* **Medikamenten-Foto-Upload:** Integration der Kamera und Galerie. Fotos realer Tablettenpackungen werden direkt in den *Firebase Storage* hochgeladen und mit dem Medikamentenobjekt verknüpft.
* **Care Circle (Kontaktbuch):** Eine zentrale Verwaltung wichtiger Bezugspersonen (Nachbarn, Hausarzt, Pflegedienst) inklusive direkter In-App-Schnellwahl für Anrufe und SMS.
* **Echtzeit-Dashboard:** Übersicht des aktuellen Tagesplans des Patienten inklusive Live-Status-Tracking (`ausstehend`, `erledigt`, `verpasst`, `in_bearbeitung`).

---

## Sicherheits- & Eskalationsarchitektur

### 1. Biometrischer App-Schutz (Android Biometric API)
Zum Schutz der Privatsphäre des Patienten ist die Admin-App durch einen biometrischen Sicherheits-Wrapper geschützt. Die App erbt von `FragmentActivity` und verlangt bei jedem Kaltstart zwingend einen **Fingerabdruck-Scan oder FaceID**, um unbefugten Zugriff auf Krankenakten zu verhindern.

### 2. Lokale Verschlüsselung (CryptoHelper)
Alle kritischen Freitexte (Namen, Dosierungen, Hinweise) werden mittels **AES-256 (CBC-Modus mit PKCS5-Padding)** direkt beim Klick auf den "Speichern"-Button lokal verschlüsselt, *bevor* sie das Smartphone in Richtung Firebase-Cloud verlassen.

### 3. Hintergrund-Überwachung & Escalation Screen
Ein Android **Foreground Service** (`AdminAlertService`) läuft permanent mit erhöhter System-Priorität im Hintergrund der Admin-App. 
* Sobald der Patient ein Medikament verpasst, fängt der Dienst die Datenbankänderung ab und löst eine **maximale Alarm-Benachrichtigung (Dauervibration + System-Alarmton)** aus.
* Ein Klick auf das Pop-up fängt den Intent ab und leitet den Angehörigen sofort auf den roten **Escalation Screen** weiter, um per Express-Button den Patienten anzurufen oder die Einnahme manuell als "KÜMMERE MICH" zu markieren.

---

## Technischer Stack

| Komponente | Technologie |
| :--- | :--- |
| **Programmiersprache** | Kotlin |
| **UI-Framework** | Jetpack Compose & Material Design 3 |
| **Authentifizierung** | Firebase Authentication (Secure Login) |
| **Cloud-Speicher** | Firebase Storage (für Bilddateien) |
| **Hintergrund-Dienste** | Android Foreground Service (mit `dataSync`-Deklaration für Android 14+) |
| **Sicherheit** | AndroidX Biometric Library & `javax.crypto` (AES-256) |
