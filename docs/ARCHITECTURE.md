# RadioCare — Architecture, moteur de conflits et fonctionnalités

## 1. Architecture en 5 couches

```
┌──────────────────────────────────────────────────────────────────────────┐
│ VIEW (com.radiocare.view)         Saisie / affichage console (Scanner)   │
│   MainView, PatientView, PlanningView, ... + Console, Menu, Tableau      │
├──────────────────────────────────────────────────────────────────────────┤
│ CONTROLLER (com.radiocare.controller)  Coordination Vue ↔ Services       │
│   PatientController, RendezVousController, FacturationController, ...    │
├──────────────────────────────────────────────────────────────────────────┤
│ SERVICE (com.radiocare.service)   Règles métier, calculs, validations    │
│   PatientService, RendezVousService, FacturationService, StockService,   │
│   ... + service.conflit (MoteurConflits + 6 règles)                      │
├──────────────────────────────────────────────────────────────────────────┤
│ DAO (com.radiocare.dao)           Persistance JSON (Jackson)             │
│   Dao<T> ← JsonDao<T> ; DataStore = 14 fichiers JSON                     │
├──────────────────────────────────────────────────────────────────────────┤
│ MODEL (com.radiocare.model)       POJO traversant toutes les couches     │
│   13 entités + énumérations + objets-valeur                              │
└──────────────────────────────────────────────────────────────────────────┘
```

Règles respectées :

- **View** : uniquement de l'entrée/sortie (lecture typée, menus, tableaux). Aucune règle métier :
  même la validation d'un CIN ou d'une référence est faite par le service ; la vue se contente
  d'afficher le message d'erreur renvoyé.
- **Controller** : point d'entrée unique de chaque vue ; il ne contient pas de règle métier, il
  délègue aux services (et peut agréger plusieurs services, ex. `FacturationController`
  = facturation + comptabilité).
- **Service** : toutes les règles (TVA, tiers payant, rémunérations, conflits, stock prévisionnel,
  alertes, statistiques). Les erreurs sont signalées par `ValidationException` (donnée invalide)
  ou `ConflitException` (refus du moteur, avec la liste des conflits).
- **DAO** : `JsonDao<T extends Identifiable>` générique ; aucune connaissance métier.
- **Model** : POJO sans dépendance technique (aucune annotation Jackson : le mapper est configuré
  pour sérialiser les champs, de sorte que les valeurs calculées comme `getHeureFin()` ne sont pas
  persistées).

L'assemblage (injection de dépendances manuelle, sans framework) est fait dans
`ServiceRegistry` (services) et `MainView` (contrôleurs et vues).

### Concepts POO mis en œuvre

| Concept | Où |
|---|---|
| Abstraction / héritage | `Personne` → `Patient`, `PersonnelMedical` → `MedecinRadiologue`, `ManipulateurRadio` |
| Polymorphisme | `PersonnelMedical.calculerRemuneration()` (honoraires vs salaire) ; `ContreIndication.interdit()` (méthode abstraite redéfinie par chaque constante) ; `RegleConflit.verifier()` |
| Interfaces | `Identifiable`, `Dao<T>`, `RegleConflit`, `Libelle` |
| Généricité | `Dao<T>`, `JsonDao<T>`, `Tableau<T>`, `Console.choisir<T>` |
| Composition | `MedecinRadiologue` ◆ `DisponibiliteHebdo` ; `EquipementImagerie` ◆ `InterventionMaintenance` ; `DataStore` ◆ 14 `JsonDao` ; `MoteurConflits` ◆ 6 règles |
| Encapsulation | attributs privés, getters/setters, invariants dans les méthodes métier (`debiter`, `enregistrerUtilisation`...) |
| Patron Stratégie / chaîne de règles | moteur de conflits |

## 2. Persistance JSON (F7)

| Fichier | Entité |
|---|---|
| `patients.json` | Patient |
| `medecins.json` | MedecinRadiologue |
| `manipulateurs.json` | ManipulateurRadio |
| `equipements.json` | EquipementImagerie (+ interventions de maintenance imbriquées) |
| `salles.json` | SalleRadiologie |
| `examens.json` | ExamenMedical |
| `rendezvous.json` | RendezVous |
| `comptes_rendus.json` | CompteRenduMedical |
| `factures.json` | FactureMedicale |
| `paiements.json` | Paiement |
| `consommables.json` | ConsommableMedical |
| `tele_expertises.json` | DemandeTeleExpertise |
| `commandes_fournisseurs.json` | CommandeFournisseur |
| `journal_conflits.json` | ConflitJournal (statistiques F10) |

- **Chargement automatique** : à la construction du `DataStore` (au démarrage).
- **Sauvegarde automatique** : chaque `create/update/delete` réécrit le fichier concerné. L'écriture
  est **atomique** (fichier `.tmp` puis déplacement) : un arrêt brutal ne laisse jamais un fichier à moitié écrit.
- **Fichier absent** → collection vide. **Fichier corrompu** → il est conservé sous
  `xxx.json.corrompu-AAAAMMJJ-HHMMSS`, la collection repart vide et un avertissement s'affiche au démarrage.
- **Intégrité référentielle** : assurée par les services (ex. impossible de supprimer un patient ayant
  des RDV, un équipement attribué à une salle, un examen utilisé ; un RDV réalisé est verrouillé).
- **Données de démonstration** : générées au premier lancement si le dossier est vide (52 patients
  fictifs, 4 médecins, 5 manipulateurs, 5 équipements, 5 salles, 9 examens, ~800 RDV sur 10 semaines,
  factures, paiements, comptes-rendus, télé-expertises, commandes, conflits journalisés).
  Les RDV passés passent réellement par le moteur de conflits et par `realiser()`, donc tous les
  agrégats (stocks, heures machine, soldes) sont cohérents.

## 3. Moteur de gestion des conflits (section 5)

`MoteurConflits` est appelé **avant tout enregistrement** de rendez-vous (`planifier`, `modifier`)
et **avant tout acte** (`realiser`) ; `TeleExpertiseService` l'utilise pour journaliser les conflits d'expertise.

### Algorithme

```
verifier(rdv):
  1. Résolution du contexte : patient, médecin, manipulateur, salle, équipement de la salle,
     examen — toute référence inconnue → ValidationException.
  2. autres = RDV actifs (Planifié / En Cours) sauf rdv lui-même (cas d'une modification).
  3. Pour chaque règle r des 6 règles : conflits += r.verifier(contexte)
     (toutes les règles sont exécutées : l'utilisateur voit TOUS les problèmes en une fois).
  4. Retourne la liste.

verifierOuRejeter(rdv):
  conflits = verifier(rdv)
  si non vide : journaliser chaque conflit (journal_conflits.json) puis lever ConflitException
```

Deux créneaux `[d1, f1[` et `[d2, f2[` se chevauchent ssi `d1 < f2 ET d2 < f1` (créneaux adjacents autorisés).
Les RDV annulés ou réalisés ne bloquent plus les ressources.

| # | Règle (classe) | Contrôle effectué |
|---|---|---|
| 1 | `RegleDoubleReservation` | Aucun autre RDV actif chevauchant dans la même **salle** ou sur le même **équipement**. Vérifie aussi : salle disponible, blindage plomb conforme (épaisseur ≥ minimum du type : Scanner 2 mm, RX 1,5 mm), équipement présent, **En Service**, et du type requis par l'examen. |
| 2 | `RegleChevauchementAgenda` | Le **médecin** et le **manipulateur** ne sont pas déjà affectés à un RDV chevauchant ; le créneau est couvert par le `DispoPlanning` du médecin ; le manipulateur est « Disponible » (pas en congé). Le patient n'a pas non plus deux examens simultanés. |
| 3 | `ReglePotentielHeures` | `heures déjà planifiées sur l'équipement + durée du RDV ≤ HeuresRestantesMaintenance`. |
| 4 | `RegleHabilitation` | Le manipulateur possède l'habilitation requise par le type d'équipement (IRM, Scanner, Rayons X, Échographie). |
| 5 | `RegleBlocagePatient` | Aucune contre-indication majeure du dossier n'interdit l'examen (ex. pacemaker → IRM, allergie iode → produit iodé, grossesse → examens ionisants, insuffisance rénale → tout produit de contraste) ; solde ≥ −500 TND. |
| 6 | `RegleRuptureStock` | Si l'examen nécessite un consommable : **stock prévisionnel** à la date ≥ quantité requise. |

**Stock prévisionnel** à la date D = stock actuel (0 si le lot est périmé à D)
\+ commandes en cours dont la livraison prévue est ≤ D
− quantités réservées par les RDV actifs datés ≤ D.

**Contrôle à la réalisation** : l'équipement doit être en service, avoir le potentiel d'heures,
et le stock **réel** doit couvrir le consommable ; sinon `ConflitException` (journalisée).
On ne peut réaliser ni démarrer un RDV daté dans le futur.

**Conflits d'expertise (F9)** : demande en attente déjà existante pour le même examen, ou expert
déjà occupé sur un créneau de 30 min chevauchant → *double demande* ; compte-rendu **Urgent** transmis
plus de 24 h après la fin de l'examen → *délai légal dépassé*.

## 4. Fonctionnalités

| Réf. | Fonctionnalité | Implémentation |
|---|---|---|
| F1 | Patients et dossiers | CRUD + recherche multicritère ; fiche avec historique d'imagerie (examen, statut, résultat du CR, montant), montant global des actes, part assurance, déjà réglé, reste à payer, solde. (`PatientService`, `PatientView`) |
| F2 | Équipements, salles, maintenance | CRUD équipements/salles ; contrôle de compatibilité salle ↔ équipement et de conformité de blindage ; décompte automatique des heures à chaque examen ; rapport d'utilisation (heures restantes, heures déjà planifiées, % de potentiel) ; alerte au secrétariat si restant ≤ max(50 h, 10 %) ; passage automatique « En Maintenance » à 0 h ; interventions avec référence textuelle de la pièce justificative et coût (intégré au F5). |
| F3 | Personnel et rémunérations | CRUD médecins (spécialité, % sur actes, planning) et manipulateurs (habilitations, tarif, statut) ; saisie des heures de garde ; fin de mois : honoraires = % × Σ tarifs HT des actes réalisés ; salaire = tarif × heures d'actes + 1,5 × tarif × heures de garde. |
| F4 | Planification et conflits | Planning hebdomadaire au format **[Heure – Patient – Examen – Salle – Équipement – Médecin – Manipulateur]** avec navigation, planning du jour, création / modification / simulation, cycle Planifié → En Cours → Réalisé / Annulé. |
| F5 | Comptabilité | Facture générée automatiquement à la réalisation : HT, **TVA 19 %**, TTC, part assurance (TTC × taux de prise en charge), part patient. Paiements (pas de trop-perçu) avec mise à jour automatique du solde et du statut (Réglée / En Attente / Retard après échéance de 30 jours). Bilan mensuel : recettes (encaissements patients + parts assurances) − dépenses (maintenance, salaires, honoraires médecins, honoraires experts, achats de consommables), TVA collectée isolée ; évolution sur 6 mois. |
| F6 | Saisie textuelle des preuves | Références normalisées (majuscules, espaces → tirets) et validées : segments alphanumériques séparés par `-` ou `/`, 4–40 caractères (ordonnances, codes actes, factures fournisseurs, pièces de maintenance, experts, normes de blindage). Les preuves de paiement doivent commencer par le préfixe du mode : `REC-` (espèces), `CHQ-`, `CB-`, `VIR-`. |
| F7 | Persistance JSON | Voir §2. |
| F8 | Stocks | CRUD consommables ; décrément automatique à la réalisation ; alertes stock critique (< seuil) avec quantité de réappro. suggérée et péremption (≤ 30 j) ; commandes fournisseurs (montant calculé, référence normée), réception (réintégration en stock + nouvelle péremption), annulation ; dépenses intégrées au F5 ; conflit 6. |
| F9 | Télé-expertise | Demande sur un examen réalisé disposant d'un CR : intégration automatique de l'ordonnance et du CR initial ; créneau planifié ; réponse textuelle de l'expert ; honoraires intégrés au F5 ; conflits d'expertise ; alerte si un avis urgent n'est pas reçu sous 24 h. |
| F10 | Statistiques | Taux d'occupation par salle et par équipement / type d'équipement (IRM vs Scanner) = minutes réservées / minutes d'ouverture (lun.–sam., 8 h–19 h) ; attente moyenne en salle (arrivée → prise en charge) et délai moyen d'obtention d'un RDV ; conflits interceptés par type (journal) ; rentabilité par examen (CA HT − coût des consommables) ; export **JSON** et **texte** dans `exports/`. |

Le centre de notifications (menu 2) regroupe : équipements en panne/maintenance, maintenance
proche, salles non conformes, stock critique, péremption, factures en retard, avis urgents en retard.

## 5. Interprétations des points ambigus du cahier des charges

1. **Solde du compte** : `soldeCompte = paiements − parts patient facturées` (négatif = débiteur).
   « Solde débiteur trop élevé » = solde < −500 TND (`ParametresCentre.SEUIL_SOLDE_DEBITEUR`).
2. **Contre-indication majeure** : énumération `ContreIndication` du dossier patient, chaque valeur
   sachant quels examens elle interdit.
3. **Taux de couverture assurance** : attribut ajouté au patient (le cahier ne mentionne que le n° d'assurance),
   nécessaire au calcul de la part assurance.
4. **Heures de garde** : saisies par mois et par manipulateur, majorées de 50 %.
5. **Délai légal de transmission** d'un compte-rendu urgent : 24 h après la fin de l'examen.
6. **Temps d'attente des patients** : deux indicateurs (attente en salle et délai d'obtention du RDV).
7. **Recettes assurances** : la part assurance est comptée en recette le mois d'émission de la facture
   (tiers payant) ; les encaissements patients le mois du paiement.
8. Attributs ajoutés pour rendre les règles applicables : type d'équipement et consommable requis
   par examen, épaisseur de plomb des salles, intervalle de maintenance, horodatages d'arrivée/prise
   en charge des RDV, date de livraison prévue des commandes, créneau des télé-expertises.
