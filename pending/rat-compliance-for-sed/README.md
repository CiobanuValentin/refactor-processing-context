# Parcat: rezultatul rat pentru sed

Cod scris fără să fie comparat cu legacy, scos din fluxul principal. Nu se integrează până nu e discutat.

- În legacy, sed găsea `rat.compliance` pe aplicația din Elasticsearch, unde îl scria rat.
- În cloud, `ApplicationBuilderPG` din sed nu cere nimic de la rat.
- Clasele de aici erau o propunere: un GET în rat și un mapper în sed. Folosesc `ProxyResultDto`, care nu mai există, deci nu compilează în forma asta.

De lămurit cu poze din legacy și cloud: cum ajunge rezultatul rat la sed și în ce format.
