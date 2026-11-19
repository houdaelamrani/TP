# TP 19 — Spring Cloud : Eureka, Gateway et OpenFeign
### Gestion des rendez-vous médicaux (Patient / Rendez-vous)

**Réalisée par : El Amrani Houda** — EMSI Marrakech

## Architecture

| Application | Port | Rôle |
|---|---|---|
| `eureka-server` | 8761 | Registre des services (Eureka) |
| `service-patient` | 8088 | CRUD de lecture des patients (`SERVICE-PATIENT`) |
| `service-rdv` | 8089 | Rendez-vous médicaux, enrichis avec le patient via OpenFeign (`SERVICE-RDV`) |
| `api-gateway` | 8888 | Point d'entrée unique, profils `static` ou `discovery` |

Entités :
- `Patient` : `id`, `nom`, `age`
- `RendezVous` : `id`, `medecin`, `specialite`, `motif`, `patientId` (le `patient` est récupéré à distance par Feign, non persisté)

## Endpoints (via la Gateway, port 8888)

| Méthode | URL | Description |
|---|---|---|
| GET | `/patients`, `/patient/{id}` | Liste / détail des patients |
| GET | `/rdv`, `/rdv/{id}` | Liste / détail des rendez-vous (avec patient) |
| GET | `/rdv/patient/{id}` | Rendez-vous d'un patient |
| POST | `/rdv/{patientId}` | Créer un rendez-vous pour un patient |
| PUT | `/rdv/{id}` | Modifier un rendez-vous |

Exemple de corps JSON :

```json
{ "medecin": "Dr Benjelloun", "specialite": "Cardiologie", "motif": "Bilan cardiaque" }
```

## Lancement

Ordre : Eureka → service-patient → service-rdv → api-gateway.

```bash
cd eureka-server   && ./mvnw spring-boot:run
cd service-patient && ./mvnw spring-boot:run
cd service-rdv     && ./mvnw spring-boot:run
cd api-gateway     && ./mvnw spring-boot:run -Dspring-boot.run.profiles=discovery
```

Test d'intégration complet (Windows, après `mvnw package` des quatre projets) : `scripts/Test-Integration.ps1`.

## Captures d'écran à ajouter (dossier `images/`)

1. **Eureka — services enregistrés** : http://localhost:8761
2. **Microservice Patient — liste des patients** : http://localhost:8088/patients
3. **Microservice Rendez-vous — liste des rendez-vous** : http://localhost:8089/rdv
4. **API Gateway — routage vers SERVICE-PATIENT** : http://localhost:8888/patients
5. **API Gateway — routage vers SERVICE-RDV** : http://localhost:8888/rdv

<!-- Remplacer par : ![Eureka](images/01_eureka_services.png) etc. -->
