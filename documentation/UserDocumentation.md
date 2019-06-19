# Alexa - Financial Assistant

Dieser Skill erweitert Amazons Sprachassistenten Alexa um verschiedene Funktionen aus dem Finanzbereich. Neben dem Abruf von grundlegenden Konto- und Depotinformationen können Überweisungen getätigt und Kredit- sowie EC-Karten verwaltet werden. Außerdem erlaubt der Skill die Erstellung von Sparplänen und Daueraufträgen.

## Installation (dev)

- Überprüfe ob config datei vorhanden ist: configuration.AmosConfiguration
- Überprüfe ob backend funktioniert: 
    * [bank server](http://ec2-18-184-56-83.eu-central-1.compute.amazonaws.com)
    * [keycloak authentication server](https://amos.franken-hosting.de/auth)
- Configuriere Alexa Skill in Developer Console: 
    * importiere die schema.json file unter amosalexa.model
    * configuriere die Account Linking mit keycloak (Authentication Code Grant)
    * configuriere Endpoint mit Lambda function in dem nächsten Schritt
- Configuriere Lambda Function `testAmos` mit handler=AmosStreamHandler und skill id, amazon s3 bucker `amos-alexa` wie in update_lambda.sh
- Run `DemoAccount`, um einen demo Account anzulegen bzw. die Daten in Datenbank im Überblick zu haben
- Run `gradle build`: wird den Code zusammenbauen und automatisch den Lambda function aktualisieren
- Amos kann jetzt im Developer Console unter Test getestet werden. Der default VoicePin = 1234, Der tan kann von Authentication App generiert werden ([qr code](https://chart.googleapis.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth%3A%2F%2Ftotp%2Fissuer%3AaccountName%3Fsecret%3DBIRAKXJN42SDFMMU%26issuer%3Dissuer) ).
- Alexa Skill, bzw. Skill Test kann entweder in der Developer Console gebaut und getestet, oder auch in ASK cli usw.


## Grundlegende Verwendung

Die Benutzung des Skills erfolgt per Spracheingabe auf unterstützten Geräten (Echo und Echo Dot) nach folgendem Muster:

`Alexa, sage AMOS ...`

`Alexa, frage AMOS ...`

Zum Beispiel:

`Alexa, frage AMOS nach meiner Bankadresse.`

`Alexa, frage AMOS wie sind meine Daueraufträge.`

## Funktionen

Eventuelle Rückfragen sind ebenfalls per Spracheingabe zu beantworten.

- Grundlegende Kontoinformationen
  - Informationen zu Kontostand, IBAN, Zinssatz, Kreditlimit etc.
  - `Alexa, frage AMOS wie ist mein Kontostand / Zinssatz / Kreditlimit / ...`
- Bestellung einer Ersatzkarte
  - `Alexa, sag AMOS ich brauche eine Ersatzkarte.`
- Sperrung einer Karte
  - `Alexa, sag AMOS sperre die Karte 123.`
- Bankadresse / Öffnungszeiten
  - `Alexa, frag AMOS nach meiner Bankadresse.`
  - `Alexa, frag AMOS wann hat die Sparkasse am Montag geöffnet.`
- Erstellung eines persönlichen Sparplans
  - `Alexa, sag AMOS berechne mir einen Sparplan.`
  - `Alexa, sag AMOS ich möchte einmalig 1000 Euro anlegen.`
- Abfrage der Daueraufträge
  - `Alexa, frag AMOS wie lauten meine Daueraufträge.`
- Bearbeitung von Daueraufträgen
  - `Alexa, sag AMOS ändere den Betrag von Dauerauftrag Nummer 1 auf 10 Euro.`
  - `Alexa, sag AMOS ändere den Ausführungsrhythmus von Dauerauftrag Nummer 1 auf monatlich, beginnend am 1. Juli.`
- Depotinformationen
  - `Alexa, frag AMOS wie ist der Stand meiner Aktien.`
  - `Alexa, frag AMOS nach dem Depotstatus.`
- Durchführen von Überweiseungen
  - `Alexa, sag AMOS überweise 10 Euro an Christian.`
