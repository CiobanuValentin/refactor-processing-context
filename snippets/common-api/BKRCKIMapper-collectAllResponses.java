// De corectat in BKRCKIMapper din cloud: in collectAllResponses, ramura a doua are argumentele inversate.
//
// Legacy: cand persoana NOUA are aliasuri (si cea din mapa nu), creditele si scorurile ei se adauga la persoana
// din mapa, care ramane in mapa.
// Cloud:  mergeCreditLoans(singlePerson, existing) adauga invers, in persoana noua, care nu e pusa in mapa -
// deci creditele si scorurile persoanei noi se pierd.
//
// mergeCreditLoans(target, source) si mergeBkrScores(target, source) raman neschimbate.

        if (existing.getAliases() != null) {
            mergeCreditLoans(singlePerson, existing);
            mergeBkrScores(singlePerson, existing);
            listPersons.put(personId, singlePerson);
        } else if (singlePerson.getAliases() != null) {
            mergeCreditLoans(existing, singlePerson);   // era (singlePerson, existing)
            mergeBkrScores(existing, singlePerson);     // era (singlePerson, existing)
        }

// O diferenta mica ramane fata de legacy: legacy sarea peste adaugare doar cand lista sursei era goala;
// cloud verifica la fel (null sau goala), deci e echivalent.
