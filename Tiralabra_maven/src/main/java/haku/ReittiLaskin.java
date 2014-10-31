/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package haku;

import verkko.Kaari;
import verkko.Linja;
import verkko.Pysakki;
import verkko.Verkko;

/**
 * A*-haun apuolio. Arvioi jäljellä olevaa kustannusta ja laskee kustannuksen
 * kustakin siirtymästä
 *
 * @author E
 */
public class ReittiLaskin {
    /**
     * Verkko, jossa laskentaa suoritetaan
     */
    private Verkko verkko;
    /**
     * Paino matka-ajan kustannukselle
     */
    private double aikaPaino;
    /**
     * Paino matkan pituuden kustannukselle
     */
    private double matkaPaino;
    /**
     * Odotusajan lisäksi lisätään tämä kustannukseen
     */
    private double vaihtoPaino;
    /**
     * Paino matkan pituudelle heuristiikkaa varten
     */
    private double heurMatkaPaino;
    /**
     * Paino matka-ajalle heuristiikkaa varten
     */
    private double heurAikaPaino;
    /**
     * Matka metreinä, aika minuutteina. Yksikkönä m/min
     */
    private double heurKulkunopeus;

    /**
     * Konstruktori omien arvojen asettamiseen
     *
     * @param aikaPaino Kustannuksen ajan painotus. Arvot epänegatiivisia
     * @param matkaPaino Kustannuksen matkan painotus. Arvot epänegatiivisia
     * @param vaihtoPaino Kustannuksen vaihdon painotus. Arvot epänegatiivisia.
     * Tämä odotusajan lisäksi kustannukseen
     * @param heurAikaPaino Heuristiikan ajan painotus. Arvot epänegatiivisia
     * @param heurMatkaPaino Heuristiikan matkan painotus. Arvot epänegatiivisia
     * @param heurKulkunopeus Heuristiikan arvioitu matkustusnopeus. Arvot
     * positiivisia
     */
    public ReittiLaskin(double aikaPaino, double matkaPaino, double vaihtoPaino, double heurAikaPaino, double heurMatkaPaino, double heurKulkunopeus) {
        this.aikaPaino = aikaPaino;
        this.matkaPaino = matkaPaino;
        this.vaihtoPaino = vaihtoPaino;
        this.heurMatkaPaino = heurMatkaPaino;
        this.heurAikaPaino = heurAikaPaino;
        this.heurKulkunopeus = heurKulkunopeus;
    }

    /**
     * Oletuskonstruktorilla painotetaan vain matka-aikaa
     */
    public ReittiLaskin() {
        // this(1,0,0,0,0,400);
        this.aikaPaino = 1;
        this.matkaPaino = 0;
        this.vaihtoPaino = 0;       // aina kun vaihdetaan linjaa, lisätään kustannukseen
        this.heurMatkaPaino = 0;
        this.heurAikaPaino = 0;     // jos h(x) == 0, saadaan tavallinen BFS
        // jos heurPainot siis == 0, kyseessä BFS: voi käyttää 
        // heuristiikkojen oikeellisuuden arviointiin (BFS vs ASTAR)
        this.heurKulkunopeus = 400; // 250 m / min -> 15km/h, 660 m / min -> 40km/h
        // kävely 100m/min -> 6km/h        
    }

    /**
     * Palauttaa annetussa verkossa solmun ja maalin välisen etäisyyden arvion.
     * Jotta toimisi, tulee olla h(n)<=d(n,k)+h(k). Painojen kanssa siis pitää
     * olla tarkkana.
     *
     * @param verkko
     * @param solmu
     * @param maali
     * @return
     */
    public double heuristiikka(Pysakki solmu, Pysakki maali) {
        double etaisyys = Math.pow(solmu.getX() - maali.getX(), 2) + Math.pow(solmu.getY() - maali.getY(), 2);
        etaisyys = Math.pow(etaisyys, 0.5);
        return heurAikaPaino * etaisyys / heurKulkunopeus + heurMatkaPaino * etaisyys;
    }

    /**
     * Laskee kaaren kulkemisen kustannuksen ottaen huomioon valitut
     * preferenssit
     *
     * @param verkko
     * @param kuljettu Reitti jota pitkin on edetty tähän
     * @param uusi Seuraavaksi kuljettava kaari
     * @return
     */
    public double kustannus(Reitti kuljettu, Kaari uusi) {
        return this.getOdotusAika(kuljettu, uusi) * aikaPaino // odotusaika 
                + uusi.getKustannus() * aikaPaino // aika
                + uusi.getEtaisyys() * matkaPaino // jos halutaan minimoida kuljettua matkaa
                + this.getOnkoVaihto(kuljettu, uusi) // jos halutaan minimoida vaihtoja
                + this.getSopivuus(uusi);                //
    }

    /**
     * Voidaan asettaa vältettäväksi tietyn tyyppiset linjat
     *
     * @param uusi Seuraavaksi kuljettava kaari
     * @return
     */
    public double getSopivuus(Kaari uusi) {
        double sopivuus = 0;
        /* // WIP
         Linja linja = verkko.getLinja( uusi.getLinjanNimi() );
         if ( linja!=null && linja.getTyyppi()!=null ) {
         if ( sopivuudet.hasKey(linja.getTyyppi()) )
         sopivuus+=sopivuudet.get(linja.getTyyppi());
         }
         */
        return sopivuus;
    }

    /**
     * Laskee uuden kaaren kulkemiseen liittyvän odotusajan. Jos pysytään
     * samalla linjalla, ei tarvitse laskea.
     *
     * @param kuljettu
     * @param uusi
     * @return
     */
    public double getOdotusAika(Reitti kuljettu, Kaari uusi) {
        double odotusAika = 0;

        if (kuljettu.getKuljettuKaari() == null
                || uusi.getLinjanNimi() == null
                || !kuljettu.getKuljettuKaari().getLinjanNimi().equals(uusi.getLinjanNimi())) { // vaihdetaan pysäkillä linjaa
            // lisätään odotusAikaan kaaren linjan saapumisajan ja tämänhetkisen ajan erotus
            odotusAika = verkko.getOdotusAika(kuljettu.getAika(), kuljettu.getSolmu().getKoodi(), uusi.getLinjanNimi());
        }
        return odotusAika;
    }

    /**
     * Jos reitillä on vaihto, palautetaan vaihtopaino.
     *
     * @param kuljettu
     * @param uusi
     * @return
     */
    public double getOnkoVaihto(Reitti kuljettu, Kaari uusi) {
        if (kuljettu.getKuljettuKaari() == null) {    // ensimmäinen pysäkki, ei vaihto
            return 0;
        } else if (uusi.getLinjanNimi() == null
                || !kuljettu.getKuljettuKaari().getLinjanNimi().equals(uusi.getLinjanNimi())) { // vaihdetaan pysäkillä linjaa
            return this.vaihtoPaino;
        }
        return 0;
    }

    // automaattiset metodit
    public Verkko getVerkko() {
        return verkko;
    }

    public void setVerkko(Verkko verkko) {
        this.verkko = verkko;
    }

    public double getAikaPaino() {
        return aikaPaino;
    }

    public void setAikaPaino(double aikaPaino) {
        this.aikaPaino = aikaPaino;
    }

    public double getMatkaPaino() {
        return matkaPaino;
    }

    public void setMatkaPaino(double matkaPaino) {
        this.matkaPaino = matkaPaino;
    }

    public double getVaihtoPaino() {
        return vaihtoPaino;
    }

    public void setVaihtoPaino(double vaihtoPaino) {
        this.vaihtoPaino = vaihtoPaino;
    }

    public double getHeurMatkaPaino() {
        return heurMatkaPaino;
    }

    public void setHeurMatkaPaino(double heurMatkaPaino) {
        this.heurMatkaPaino = heurMatkaPaino;
    }

    public double getHeurAikaPaino() {
        return heurAikaPaino;
    }

    public void setHeurAikaPaino(double heurAikaPaino) {
        this.heurAikaPaino = heurAikaPaino;
    }

    public double getHeurKulkunopeus() {
        return heurKulkunopeus;
    }

    public void setHeurKulkunopeus(double heurKulkunopeus) {
        this.heurKulkunopeus = heurKulkunopeus;
    }

    @Override
    public String toString() {
        return "ReittiLaskin{" + "aikaPaino=" + aikaPaino + ", matkaPaino=" + matkaPaino + ", vaihtoPaino=" + this.vaihtoPaino + ", heurMatkaPaino=" + heurMatkaPaino + ", heurAikaPaino=" + heurAikaPaino + ", heurKulkunopeus=" + heurKulkunopeus + '}';
    }

}
