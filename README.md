# RadioCare

Application **console** Java de gestion d'un centre de radiologie et d'imagerie médicale.
Mini-projet POO, CPI-2, Faculté des Sciences de Bizerte (Université de Carthage), 2026-2027.

- Architecture stricte en **5 couches** : Model → DAO → Service → Controller → View (console).
- **13 entités** : les 10 obligatoires et les 3 de l'extension trinôme.
- Fonctionnalités **F1 à F10**, plus un **moteur de conflits à 6 règles**.
- **Persistance JSON** automatique : un fichier par entité.
- **64 tests JUnit 5**.

## Prérequis

| Outil | Version |
|---|---|
| JDK | 21 ou plus récent (testé avec OpenJDK 25, compilé en `--release 21`) |
| Maven | inutile à installer : le wrapper `./mvnw` le télécharge (ou Maven ≥ 3.9) |

Dépendances, téléchargées automatiquement : Jackson 2.17 (JSON) et JUnit 5.10 (tests). Le projet n'utilise aucun framework ni aucune base de données.

## Compilation, tests et exécution

Depuis le dossier `radiocare/` :

```bash
./mvnw clean package
```

Cette commande compile le projet, lance les 64 tests et produit `target/radiocare-1.0.0-all.jar`.

```bash
java -jar target/radiocare-1.0.0-all.jar
```

Autres possibilités :

```bash
./mvnw test                 # tests seulement
./mvnw -q compile exec:java # lancement sans construire le jar
```

Sous Windows, utilisez `mvnw.cmd`.

Options de lancement :

| Option | Effet |
|---|---|
| `--data <dossier>` | dossier des fichiers JSON (défaut : `./data`) |
| `--exports <dossier>` | dossier des rapports exportés (défaut : `./exports`) |
| `--sans-demo` | ne pas générer les données de démonstration au premier lancement |

Au **premier lancement**, si `data/` est vide, un jeu de données fictif cohérent est généré : 52 patients, 4 médecins, 5 manipulateurs, 5 équipements, 5 salles, environ 800 rendez-vous sur 10 semaines, ainsi que les factures, paiements, comptes-rendus, télé-expertises et commandes correspondants. Pour repartir de zéro, supprimez le dossier `data/`.

Les couleurs ANSI sont désactivées automatiquement hors terminal, ou si la variable `NO_COLOR` est définie.

## Utilisation

```
 1. Tableau de bord                       7. Catalogue des examens
 2. Centre de notifications (alertes)     8. Comptes-rendus médicaux (F6)
 3. Patients & dossiers médicaux (F1)     9. Facturation, paiements & bilan (F5)
 4. Équipements, salles & maintenance (F2) 10. Stocks & commandes (F8)
 5. Personnel & rémunérations (F3)       11. Télé-expertise (F9)
 6. Rendez-vous, planning & conflits (F4) 12. Statistiques & rapports (F10)
```

Conventions de saisie :

- **Entrée** conserve la valeur proposée entre crochets.
- **`!`** annule l'opération en cours.
- `-` vide un champ facultatif.
- Les dates s'écrivent `jj/mm/aaaa` (`auj` pour aujourd'hui) et les heures `HH:mm`.
- Pour choisir un patient, un examen, etc., tapez son identifiant ou une partie de son nom. Laissez vide pour afficher la liste numérotée.

### Scénario de démonstration du moteur de conflits

1. Menu 6, puis 3 (**Nouveau rendez-vous**). Choisissez le patient `Gabsi`, l'examen `IRM-CER` (IRM cérébrale) et le manipulateur **Nour Ben Ammar** (non habilité IRM). Le moteur refuse le RDV (**Conflit 4**) et le journalise.
2. Même saisie avec le patient **Ben Salah Hedi** (solde ≈ −904 TND) : refus pour **Conflit 5** (blocage financier).
3. Patient **Saidi Mohamed** (porteur de pacemaker) avec une IRM : **Conflit 5** (contre-indication).
4. Salle **Écho B** : l'échographe est en panne, d'où un **Conflit 1**.
5. Menu 12, puis 3 : affiche le journal de tous les conflits interceptés.

## Structure du projet

```
radiocare/
├── pom.xml, mvnw, mvnw.cmd, .mvn/
├── docs/
│   ├── ARCHITECTURE.md          architecture, persistance, algorithme des conflits, fonctionnalités
│   └── diagramme-classes.puml   diagramme de classes UML (PlantUML)
└── src/
    ├── main/java/com/radiocare/
    │   ├── Main.java            point d'entrée
    │   ├── model/               13 entités + enums + objets-valeur (POJO)
    │   ├── dao/                 Dao<T>, JsonDao<T>, DataStore, JsonMapperFactory
    │   ├── service/             services métier, ServiceRegistry, DemoDataService
    │   │   └── conflit/         MoteurConflits + 6 règles (RegleConflit)
    │   ├── controller/          10 contrôleurs
    │   └── view/                vues console + Console (Scanner), Menu, Tableau
    └── test/java/com/radiocare/ tests JUnit 5 (DAO + services)
```

L'architecture, l'algorithme de détection des conflits, la description de chaque fonctionnalité et les **interprétations retenues pour les points ambigus** sont détaillés dans [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Pour visualiser le diagramme UML, ouvrez `docs/diagramme-classes.puml` avec l'extension PlantUML de VS Code ou IntelliJ, ou sur plantuml.com.

## Correspondance avec le cahier des charges

| Exigence | Où |
|---|---|
| 5 couches | packages `model`, `dao`, `service`, `controller`, `view` |
| Entités 1–10 | `Patient`, `MedecinRadiologue`, `ManipulateurRadio`, `EquipementImagerie`, `SalleRadiologie`, `ExamenMedical`, `RendezVous`, `CompteRenduMedical`, `FactureMedicale`, `Paiement` |
| Entités 11–13 | `ConsommableMedical`, `DemandeTeleExpertise`, `CommandeFournisseur` |
| CRUD complet des 13 entités | services + menus (Ajouter / Modifier / Supprimer / Lister) |
| F1 bilan financier patient | `PatientService.bilan()`, menu 3 → 3 |
| F2 alerte maintenance / rapport d'utilisation | `EquipementService.rapportUtilisation()`, `AlerteService` |
| F3 honoraires / salaires | `PersonnelMedical.calculerRemuneration()` (polymorphe), menu 5 → 10 |
| F4 planning [Heure-Patient-Examen-Salle-Équipement-Médecin-Manipulateur] | `PlanningView`, menu 6 → 1 |
| F5 TVA 19 %, tiers payant, bilan mensuel | `FacturationService`, `ComptabiliteService` |
| F6 références textuelles normées | `Validateur.reference()` / `preuvePaiement()` |
| F7 JSON | `JsonDao`, `DataStore` (14 fichiers) |
| F8 stocks / commandes | `StockService` |
| F9 télé-expertise | `TeleExpertiseService` |
| F10 occupation, attente, conflits, rentabilité, export JSON + texte | `StatistiqueService` |
| Conflits 1–6 | `service/conflit/Regle*.java`, `MoteurConflits` |

## Tests

| Classe | Couverture |
|---|---|
| `JsonDaoTest` | CRUD, identifiants, persistance/rechargement, fichier absent et fichier corrompu |
| `MoteurConflitsTest` | les 6 règles (cas positifs et négatifs), cumul, journalisation, modification, annulation |
| `FacturationServiceTest` | TVA, tiers payant, paiements partiels ou totaux, trop-perçu, preuves, retard, blocage d'un débiteur |
| `RendezVousServiceTest` | cycle de vie complet, décompte des heures, stock, historique, planning hebdomadaire |
| `EquipementServiceTest` | usure, alertes, maintenance, compatibilité des salles, blindage |
| `PersonnelServiceTest` | honoraires, salaires et heures de garde, validations |
| `StockServiceTest` | consommation, alertes, stock prévisionnel, commandes |
| `TeleExpertiseServiceTest` | intégration du CR, réponse, double demande, délai d'urgence |
| `PatientServiceTest` | CRUD, validations, historique |
| `StatistiqueServiceTest` | occupation, attente, rentabilité, bilan, exports |
| `DemoDataServiceTest` | génération des données de démonstration et rechargement sans perte |

## Limites connues

- Application mono-utilisateur. L'accès concurrent aux fichiers JSON par deux instances n'est pas géré.
- Les données sont rechargées entièrement en mémoire, ce qui convient au volume d'un centre (quelques milliers d'enregistrements).
- Pas d'authentification ni de gestion des rôles : le cahier des charges ne les demande pas.
- Les paramètres du centre (horaires 8 h–19 h, seuil débiteur 500 TND, échéance 30 jours…) sont des constantes de `ParametresCentre`. Ils ne sont pas modifiables depuis l'interface.
# RadioCare
