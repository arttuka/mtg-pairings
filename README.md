# MTG-Pairings

MTG-pairings on ohjelma, jolla saa lähetettyä Wizards Event Reporterista turnauksen pairingit ja tulokset osoitteessa [https://pairings.fi](https://pairings.fi) sijaitsevalle palvelimelle.

### Vaatimukset

* Wizards Event Reporter

### Käyttö

Ohjelman käynnistyessä aukeaa ikkuna, jossa kysytään API keytä. Se on MtgSuomi-tunnukseen sidottu ja löytyy menemällä osoitteeseen [https://mtgsuomi.fi/apikey](https://mtgsuomi.fi/apikey). API keyn voit syöttää myös ikkunan yläreunan valikosta.

Ohjelman etusivulla on lista WERistä löytyvistä turnauksista. Valitse haluamasi turnaus laittamalla rasti turnauksen nimen edellä olevaan ruutuun. Oletuksena listassa näkyvät vain aktiiviset turnaukset (eli turnaukset, joiden Enrollment Complete -nappia on painettu ja jonka End Event -nappia ei ole painettu). Yläreunan ruudussa olevan rastin poistamalla saa näkyviin kaikki turnaukset aikajärjestyksessä.

Ohjelma lähettää kaikki turnauksen muutokset automaattisesti, ja oikean laidan logiin päivittyy lista tapahtumista. Jos haluat vaihtaa turnauksen nimeä, klikkaa sitä vasemman reunan listasta, vaihda nimi ohjelman alareunan tekstikenttään ja klikkaa Tallenna.

### Bugit, uusi toiminnallisuus ja kontribuutiot

Jos löydät bugin tai haluat ohjelmaan uutta toiminnallisuutta, tee tiketti [issue trackeriin](https://github.com/arttuka/mtg-pairings/issues). Kirjoita issuelle otsikko ja kuvaus, valitse sille sopiva tyyppi (bug/enhancement) ja lähetä se.

Jos haluat auttaa ohjelman kehittämisessä, voit koodata haluamasi toiminnallisuuden (tee issue) ja tehdä siitä pull requestin.

### Lisenssi

Kaikkien ohjelman osien lähdekoodi on julkaistu MIT-lisenssillä.
