# MTB File to Kafka producer
Accept MTB JSON via REST endpoint. Produces File to Kafka topic. Optional Patient PID will be replaced by gPas pseudonym, other identifier can be hashed.

## Dependency Kafka
* Apache Kafka® 3.4 broker connection.

## configuration

| configuration key                       | description                                                                                               |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------|
| mtb2kafka.mtbProducerOutput.destination | target output topic in kafka where mtb files will be produced to                                          |
| mtb2kafka.pseudonym.enabled             | if set to `true`, patient ID will be replaced with a gPas pseudonym. Other ID properties will be hashed.  |
| mtb2kafka.pseudonym.gPasUrl             | base url to your gPas instance                                                                            |
| mtb2kafka.pseudonym.target              | gPas domain name for patient pseudonym creation                                                           |
| server.port                             | configured port of this producer REST endpoint                                                            |

## Run Dev

1. To run Kafka goto `./deploy` and run `docker compose -f docker-compose.dev.yml up -d`
2. If you like, you can also download and run gPas: read `README_TEST_WITH_GPAS.md`

## Use

Currently, there is only one endpoint available `/mtbfile`.

POST `http://localhost:8880/mtbfile` 

Example json:

```json
{
  "patient": {
    "id": "internal-ID-2234567",
    "gender": "male",
    "birthDate": "1975-01",
    "insurance": "Barmer"
  },
  "consent": {
    "id": "db2e9f62-9e5a-481b-b0d5-21a058f1b8fc",
    "patient": "internal-ID-2234567",
    "status": "active"
  },
  "episode": {
    "id": "4c82c5c9-19b4-41d2-817b-5941b25c0c44",
    "patient": "internal-ID-2234567",
    "period": {
      "start": "2023-06-28"
    }
  },
  "diagnoses": [
    {
      "id": "4249510f-ccba-4c65-88b8-064567564cfc",
      "patient": "internal-ID-2234567",
      "recordedOn": "2023-06-28",
      "icd10": {
        "code": "C26.8",
        "display": "Bösartige Neubildung: Verdauungssystem, mehrere Teilbereiche überlappend",
        "version": "2022",
        "system": "ICD-10-GM"
      },
      "icdO3T": {
        "code": "C26.8",
        "display": "Verdauungssystem, mehrere Bereiche überlappend",
        "version": "Zweite Revision",
        "system": "ICD-O-3-T"
      },
      "whoGrade": {
        "code": "III",
        "system": "WHO-Grading-CNS-Tumors"
      },
      "histologyResults": [

      ],
      "statusHistory": [
        {
          "status": "unknown",
          "date": "2023-06-28"
        },
        {
          "status": "metastasized",
          "date": "2023-06-28"
        }
      ],
      "guidelineTreatmentStatus": "unknown"
    }
  ]
}

```
