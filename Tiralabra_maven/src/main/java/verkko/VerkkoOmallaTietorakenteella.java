/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verkko;
import tira.DynaaminenLista;
import tira.Hajautustaulu;
import tira.Lista;
import verkko.esimerkki.LinjaJSON;
import verkko.esimerkki.Pysakkiverkko;
import verkko.rajapinnat.Edge;
import verkko.rajapinnat.Graph;
import verkko.rajapinnat.Node;
import verkko.rajapinnat.Value;

/**
 * Verkko. Tähän on vaihdettu omat tietorakenteet javan valmiiden sijalle.
 * 
 * @author E
 */
public class VerkkoOmallaTietorakenteella extends Verkko /*implements Graph*/ {
    
    /**
     * Pysäkit taulukossa
     */
    private Pysakki[] pysakit;
    /**
     * Linjat taulukossa
     */
    private Linja[] linjat;
    
    ////////////////////////
    // hakuja nopeuttamaan//
    ////////////////////////
    
    /**
     * Hajautustaulu: avaimina pysäkkien koodit, arvoina pysäkki-oliot
     */
    private Hajautustaulu<String, Pysakki> pysakinKoodit;
    /**
     * Hajautustaulu: avaimina linjojen koodit, arvoina linja-oliot
     */    
    private Hajautustaulu<String, Linja> linjanKoodit;
    
    /**
     * Avaimina pysäkit, arvoina lista linjoista jotka kulkevat pysäkiltä
     */
    private Hajautustaulu<Pysakki, Lista<Linja>> pysakiltaKulkevatLinjat;
    /**
     * Avaimina pysäkit, arvoina lista pysäkeistä joihin pysäkiltä pääsee jollain linjalla
     */
    private Hajautustaulu<Pysakki, Lista<Pysakki>> pysakinNaapurit;   // max V*V
    /**
     * Avaimena pysäkit, arvoina hajautustaulu pysäkin naapureista ja lista linjojen kaarista, joilla naapureihin pääsee
     */
    private Hajautustaulu<Pysakki, Hajautustaulu<Pysakki, Lista<Kaari>>> reititNaapureihin; // max V*V*E
    
    /**
     * Pysäkkien ohitusajat. ( Tieto vuorojen tiheydestä linjassa ), pysakin
     * koodi-linjan koodi-ohitusaika
     */
    private Hajautustaulu<String, Hajautustaulu<String, Double>> pysakkiAikataulut;

    /**
     * Hivenen pitkä konstruktori. Osa toiminnallisuudesta siirretty yksityisiin metodeihin
     * Luo verkon käyttäen apuna pysäkkiverkko-oliota
     * - JSON-data luetaan Pysakkiverkko-oliolla
     * - JSON-data muokataan verkko-paketin olioiksi ja tallennetaan taulukoihin
     * - Luodaan apukentiksi hakuihin hajautustaulut, käyetään apumetodeja
     * 
     */
    public VerkkoOmallaTietorakenteella() {
        // apuolioita hakujen nopeuttamiseen
        linjanKoodit = new Hajautustaulu();
        pysakinKoodit = new Hajautustaulu();
        pysakinNaapurit = new Hajautustaulu();
        reititNaapureihin = new Hajautustaulu();
        pysakiltaKulkevatLinjat = new Hajautustaulu();
        pysakkiAikataulut = new Hajautustaulu();
        // luetaan JSON-datasta pysäkit ja linjat
        Pysakkiverkko pysakkiverkko = new Pysakkiverkko();
        pysakkiverkko.create("verkko.json", "linjat.json");
        // rakennetaan JSON-olioista tarkoituksenmukainen verkko
        // pysakit
        pysakit = new Pysakki[pysakkiverkko.getPysakit().length];
        for (int i = 0; i < pysakit.length; i++) {
            pysakit[i] = new Pysakki(pysakkiverkko.getPysakit()[i]);
            pysakinKoodit.put(pysakit[i].getKoodi(), pysakit[i]);
        }
        // linjat
        linjat = new Linja[pysakkiverkko.getLinjat().length];
        for (int i = 0; i < linjat.length; i++) {
            LinjaJSON linja = pysakkiverkko.getLinjat()[i];
            linjat[i] = new Linja(linja);
            linjat[i].setTyyppi(Linja.TYYPPI_RATIKKA);
            // linjan reitin tallentaminen: kaaret linkitettyyn listaan
            Lista<Kaari> linjanReitti = new DynaaminenLista();
            for (int j = 0; j < linja.getPsKoodit().length; j++) {
                // lisätään pysähtymistieto pysäkkiaikatauluun
                this.lisaaPysakkiAikataulut(linja.getPsKoodit()[j], linja.getPsAjat()[j], linja.getKoodi());
                if (j >= linja.getPsKoodit().length - 1) {
                    break;
                }
                // luodaan kaaret ja niiden määräämät yhteydet
                Kaari kaari = new Kaari();
                kaari.setAlkuSolmu(linja.getPsKoodit()[j]);
                kaari.setLoppuSolmu(linja.getPsKoodit()[j + 1]);
                kaari.setKustannus(linja.getPsAjat()[j + 1] - linja.getPsAjat()[j]); // aika pysäkiltä toiselle
                kaari.setEtaisyys(Math.pow(
                        Math.pow(linja.getX()[j + 1] - linja.getX()[j], 2) + Math.pow(linja.getY()[j + 1] - linja.getY()[j], 2),
                        0.5
                )); // euklidinen etäisyys R^2
                kaari.setLinjanNimi(linjat[i].getKoodi());
                linjanReitti.add(kaari);
                // päivitetään verkkoa:
                // linjaa pitkin pääsee alkupysäkiltä loppupysäkille. päivitetään siis naapureita
                this.lisaaNaapuri(kaari);
                // ... ja naapureihin johtavia reittejä
                this.lisaaReittiNaapuriin(kaari);
                // lisätään linja pysäkin kautta kulkeviin kulkeviin
                this.lisaaPysakille(kaari);
                
            }
            // linjat[i].setReitti(linjanReitti);
            linjanKoodit.put(linjat[i].getKoodi(), linjat[i]);
            
        }
        
    }
    //////////////////////////////////////
    //Konstruktorin apumetodit (private)//
    /////////////////////////////////////

    /**
     * Päivittää pysäkkiaikatauluja
     *
     * @param pysakki
     * @param aika
     * @param linja
     */
    private void lisaaPysakkiAikataulut(String p, double aika, String l) {
        
        if (!pysakkiAikataulut.containsKey(p)) {
            pysakkiAikataulut.put(p, new Hajautustaulu());
        }
        pysakkiAikataulut.get(p).put(l, aika);
    }

    /**
     * Verkon luomisessa käytetty apumetodi. Luo yhteyden solmujen(pysäkkien) välille.
     *
     * @param kaari Parametrin välittämien solmuihin lisätään yhteys
     */
    private void lisaaPysakille(Kaari kaari) {
        Pysakki alku = pysakinKoodit.get(kaari.getAlkuSolmu());
        Pysakki loppu = pysakinKoodit.get(kaari.getLoppuSolmu());
        if (!pysakiltaKulkevatLinjat.containsKey(alku)) {
            pysakiltaKulkevatLinjat.put(alku, new DynaaminenLista());
        }
        if (!pysakiltaKulkevatLinjat.containsKey(loppu)) {
            pysakiltaKulkevatLinjat.put(loppu, new DynaaminenLista());
        }
        Linja linja = linjanKoodit.get(kaari.getLinjanNimi());
        if (!pysakiltaKulkevatLinjat.get(alku).contains(linja)) {
            pysakiltaKulkevatLinjat.get(alku).add(linja);
        }
        if (!pysakiltaKulkevatLinjat.get(loppu).contains(linja)) {
            pysakiltaKulkevatLinjat.get(loppu).add(linja);
        }
    }

    /**
     * Verkon luomisessa käytetty apumetodi. Luo yhteyden solmujen välille.
     *
     * @param kaari Parametrin välittämien solmujen vällille luodaan yhteys
     */
    private void lisaaNaapuri(Kaari kaari) {
        Pysakki alku = pysakinKoodit.get(kaari.getAlkuSolmu());
        Pysakki loppu = pysakinKoodit.get(kaari.getLoppuSolmu());
        if (!this.pysakinNaapurit.containsKey(alku)) {
            Lista<Pysakki> pysakinNaapurit = new DynaaminenLista();
            pysakinNaapurit.add(loppu);
            this.pysakinNaapurit.put(alku, pysakinNaapurit);
        } else {
            if (!this.pysakinNaapurit.get(alku).contains(loppu)) {
                this.pysakinNaapurit.get(alku).add(loppu);
            }
        }
    }

    /**
     * Verkon luomisessa käytetty apumetodi. Lisää kaaren solmujen välille.
     *
     * @param kaari Parametrin välittämien solmujen vällille asetetaan kaari
     */
    private void lisaaReittiNaapuriin(Kaari kaari) {
        Pysakki alku = pysakinKoodit.get(kaari.getAlkuSolmu());
        Pysakki loppu = pysakinKoodit.get(kaari.getLoppuSolmu());
        if (!this.reititNaapureihin.containsKey(alku)) {
            this.reititNaapureihin.put(alku, new Hajautustaulu());
        }
        if (!this.reititNaapureihin.get(alku).containsKey(loppu)) {
            this.reititNaapureihin.get(alku).put(loppu, new DynaaminenLista());
        }
        this.reititNaapureihin.get(alku).get(loppu).add(kaari);
    }
    //////////////////////
    ////JULKISET METODIT//
    //////////////////////
    
    /**
     * Pysäkkien ja linjojen tulostaminen
     */
    @Override
    public void debugPrint() {
        for (Pysakki pysakki : this.getPysakit()) {
            System.out.println("" + pysakki);
        }
        for (Linja linja : this.getLinjat()) {
            System.out.println("" + linja);
        }
    }

    /**
     * Palauttaa alku- ja loppusolmujen väliset kaaret (voi olla useita)
     *
     * @param alku
     * @param loppu
     * @return Kaaret alku- ja loppusolmun välillä
     */
    @Override
    public Iterable<Kaari> getKaaret(Pysakki alku, Pysakki loppu) { 
        return this.reititNaapureihin.get(alku).get(loppu);
    }

    /**
     * Palauttaa solmun naapurit
     *
     * @param solmu
     * @return
     */
    @Override
    public Iterable<Pysakki> getNaapurit(Pysakki solmu) { 
        return this.pysakinNaapurit.get(solmu);
    }

    /**
     * Palauttaa seuraavan linjan ohitusajan pysäkiltä (pysäkkiaikataulu)
     *
     * @param aika
     * @param pysakki
     * @param linja
     * @return
     */
    @Override
    public double getOdotusAika(double aika, String pysakki, String linja) {
        
        double ohitusAika = pysakkiAikataulut.get(pysakki).get(linja);
        double vuorovali = 10; // = l.getVuoroVali( aika );

        double odotusaika = ohitusAika % vuorovali - aika % vuorovali;
        if (odotusaika < 0) {
            odotusaika += vuorovali;
        }
        return odotusaika;
    }

}
