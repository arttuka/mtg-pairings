# MTG-Pairings

MTG-pairings on ohjelma, jolla saa lähetettyä Wizards Event Reporterista turnauksen pairingit ja tulokset osoitteessa [http://p.mtgsuomi.fi](http://p.mtgsuomi.fi) sijaitsevalle palvelimelle.

### Vaatimukset

* Wizards Event Reporter
* Java 1.7

### Käyttö

![Turnauslista](http://i.imgur.com/8lZycfe.png)
![Turnaustabi](http://i.imgur.com/NVnsjtS.png)

Ohjelman käynnistyessä aukeaa ikkuna, jossa kysytään API keytä. Se on MtgSuomi-tunnukseen sidottu ja löytyy menemällä osoitteeseen [http://mtgsuomi.fi/apikey](http://mtgsuomi.fi/apikey). Jos ohjelma ei löydä WERin tietokantaa, aukeaa myös valikko tietokantatiedoston etsimistä varten.

Ohjelman etusivulla on lista WERistä löytyvistä turnauksista. Valitse haluamasi turnaus laittamalla rasti seuranta-sarakkeessa olevaan ruutuun. Taulukon voi järjestää nimen tai päivämäärän perusteella klikkaamalla taulukon otsikkoa.

Kun turnauksen on valinnut seurattavaksi, sille aukeaa toisen kuvan mukainen välilehti. Lähetä-napit lähettävät tiedot palvelimelle, ja osaavat ottaa huomioon muut vaadittavat tiedot. Esimerkiksi pairingien lähettäminen ennen tiimien lähettämistä aiheuttaa sen, että ohjelma lähettää ensin tiimit ja sitten pairingit.

Tulosten kohdalla lähetä-nappi vain lähettää tulokset, mutta standingsit eivät tule näkyviin. Standigsit saa näkymään julkaise-napilla. Tarkista-nappi antaa listan pöydistä, joilta ohjelman mielestä puuttuu tulos. Sillä voi tarkistaa, missä vika on, jos ohjelma ei anna lähettää tuloksia vaikka ne on kaikki syötetty Reporterissa.

Resetoi-nappi poistaa palvelimelta kaiken turnaukseen liittyvän datan. Sitä voi käyttää, jos ohjelma tuntuu jumiutuneen.
