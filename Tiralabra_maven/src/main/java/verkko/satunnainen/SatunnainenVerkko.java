/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verkko.satunnainen;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import tira.DynaaminenLista;
import tira.Lista;
import verkko.rajapinnat.*;

/**
 * Verkko: nxn-taulukko, jossa voi edetä koordinaattiakselien suuntaan yhden
 * ruudun kerrallaan
 *
 * @author E
 */
public class SatunnainenVerkko implements Graph {

    ///////////////////////////////////////
    // SATUNNAISTEN PAINOJEN LASKEMINEN ///
    ///////////////////////////////////////
    /**
     * Verkon painoiksi kokonaislukuja
     */
    public static final Funktio KOKONAISLUKU = new Funktio() {
        private Random random = new Random();

        public double laskeSatunnaisluku(double kerroin) {
            return (int) (kerroin * random.nextDouble());
        }
    };
    /**
     * Verkon painoiksi desimaalilukuja
     */
    public static final Funktio REAALILUKU = new Funktio() {
        private Random random = new Random();

        public double laskeSatunnaisluku(double kerroin) {
            return (kerroin * random.nextDouble());
        }
    };
    /**
     * Verkon painoiksi desimaalilukuja
     */
    public static final Funktio ESTEITA = new Funktio() {
        private Random random = new Random();
        /**
         * Katkaisut
         */
        private double a = 0.5, b = 0.90;
        private double maxArvo = 20;

        public double laskeSatunnaisluku(double kerroin) {

            double x = random.nextDouble();

            if (x < a) {
                return 0; // (kerroin * random.nextDouble());
            } else if (x > b) {
                return maxArvo * kerroin;
            } else {
                return (kerroin * random.nextDouble());
            }
        }
    };
    /**
     * Vieruslistat tallennettuna taulukkoon. Kasvattaa tilavaatimusta mutta
     * nopeuttaa hakuja.
     */
    private Lista<Value>[][] naapurit;
    /**
     * Verkko tallennetaan taulukkoon
     */
    private Value[][] verkko;
    /**
     * Kaaret verkon solmujen välissä
     */
    private Edge[][][] kaaret;
    /**
     * Kaarien painot
     */
    private double[][] painot;
    /**
     * Painojen generoinnissa käytettävät apumuuttujat
     */
    private double minimiPaino, kerroinPaino;
    /**
     * Verkon rivit, sarakkeet
     */
    private int rivit, sarakkeet;
    /**
     * Satunnaisluvun generointiin
     */
    private Funktio funktio;
    /**
     * Saako verkossa kulkea vain akselien suuntaan
     */
    private boolean liikkumisSaanto;

    /**
     * Luo verkon, jonka koko on nxn
     *
     * @param koko
     */
    public SatunnainenVerkko(int koko) {
        this(koko, koko);
    }

    /**
     * Luo annetun kokoisen verkon
     *
     * @param rivit
     * @param sarakkeet
     */
    public SatunnainenVerkko(int rivit, int sarakkeet) {
        this(rivit, sarakkeet, 1, 20);

    }

    /**
     * Luo annetun kokoisen verkon
     *
     * @param rivit
     * @param sarakkeet
     * @param minimiPaino Kaaren kulkemisen minimipaino
     * @param kerroinPaino Satunnaisluvun painotus kaarien kustannuksiin
     */
    public SatunnainenVerkko(int rivit, int sarakkeet, double minimiPaino, double kerroinPaino) {
        this(rivit, sarakkeet, minimiPaino, kerroinPaino, 1);
    }

    /**
     * Luo annetun kokoisen verkon
     *
     * @param rivit
     * @param sarakkeet
     * @param minimiPaino Kaaren kulkemisen minimipaino
     * @param kerroinPaino Satunnaisluvun painotus kaarien kustannuksiin
     * @param funktio Valitaan painojen generointitapa. 0=kokonaislukuja,
     * 1=desimaalilukuja, 2=esteitä
     */
    public SatunnainenVerkko(int rivit, int sarakkeet, double minimiPaino, double kerroinPaino, int funktio) {
        this(rivit, sarakkeet, minimiPaino, kerroinPaino, 1, true);
    }

    /**
     *
     * @param rivit
     * @param sarakkeet
     * @param minimiPaino
     * @param kerroinPaino
     * @param funktio
     * @param liikkuminen True: liikkuminen vain koordinaattiakselien suuntaan
     */
    public SatunnainenVerkko(int rivit, int sarakkeet, double minimiPaino, double kerroinPaino, int funktio, boolean liikkuminen) {
        if (funktio == 0) {
            this.funktio = KOKONAISLUKU;
        } else if (funktio == 1) {
            this.funktio = REAALILUKU;
        } else {
            this.funktio = ESTEITA;
        }
        this.minimiPaino = minimiPaino;
        this.kerroinPaino = kerroinPaino;
        this.rivit = rivit;
        this.sarakkeet = sarakkeet;
        painot = new double[rivit][sarakkeet];
        verkko = new V[rivit][sarakkeet];
        kaaret = new E[rivit][sarakkeet][1];
        naapurit = new Lista[rivit][sarakkeet];
        liikkumisSaanto = liikkuminen;
        generoiSatunnainen();
    }

    /**
     * Verkon generointi
     */
    private void generoiSatunnainen() {
        // Random random = new Random();
        for (int i = 0; i < rivit; i++) {
            for (int j = 0; j < sarakkeet; j++) {
                painot[i][j] = minimiPaino + funktio.laskeSatunnaisluku(kerroinPaino); // satunnainen paino tähän solmuun kulkemiselle
                verkko[i][j] = new V(i, j, liikkumisSaanto);
                kaaret[i][j][0] = new E(painot[i][j]);

            }
        }
        for (int i = 0; i < rivit; i++) {
            for (int j = 0; j < sarakkeet; j++) {
                naapurit[i][j] = getNaapurit(i, j);
            }
        }

    }

    /**
     * Palauttaa verkon solmun
     *
     * @param i Rivi
     * @param j Sarake
     * @return
     */
    public Value getSolmu(int i, int j) {
        return verkko[i][j];
    }

    ///////////////////////
    /// GRAPH-RAJAPINTA ///
    ///////////////////////
    /**
     * Kaaret alkusolmusta loppusolmuun
     *
     * @param alku
     * @param loppu
     * @return
     */
    public Iterable<Edge> getKaaret(Value alku, Value loppu) {
        V l = (V) loppu;
        Lista<Edge> returnvalue = new DynaaminenLista(/* kaaret[l.getX()][l.getY()].length*/1);

        for (Edge e : kaaret[l.getX()][l.getY()]) {
            returnvalue.add(e);
        }

        // returnvalue.add( new E((V)alku,(V)loppu,painot[l.getX()][l.getY()]) );
        return returnvalue;
    }

    /**
     * Solmun naapurit
     *
     * @param x Rivi
     * @param y Sarake
     * @return Naapurit listassa
     */
    public Lista<Value> getNaapurit(int x, int y) {
        Lista<Value> returnvalue = new DynaaminenLista(8);
        if (x > 0) {
            returnvalue.add(verkko[x - 1][y]);
        }
        if (x < rivit - 1) {
            returnvalue.add(verkko[x + 1][y]);
        }
        if (y > 0) {
            returnvalue.add(verkko[x][y - 1]);
        }
        if (y < sarakkeet - 1) {
            returnvalue.add(verkko[x][y + 1]);
        }
        if (liikkumisSaanto) {
            return returnvalue;
        }
        if (x < rivit - 1 && y < sarakkeet - 1) {
            returnvalue.add(verkko[x + 1][y + 1]);
        }
        if (x < rivit - 1 && y > 0) {
            returnvalue.add(verkko[x + 1][y - 1]);
        }
        if (x > 0 && y < sarakkeet - 1) {
            returnvalue.add(verkko[x - 1][y + 1]);
        }
        if (x > 0 && y > 0) {
            returnvalue.add(verkko[x - 1][y - 1]);
        }
        return returnvalue;
    }

    /**
     * Graph-rajapinnan toteutus
     *
     * @param alku
     * @return
     */
    public Iterable<Value> getNaapurit(Value alku) {
        V l = (V) alku;
        int x = l.getX();
        int y = l.getY();
        return naapurit[x][y];
    }

    /**
     * Uusi Polku-olio
     *
     * @return
     */
    public Node getNode() {
        return new Polku();
    }

    @Override
    public String toString() {
        String s = "";
        for (double[] d : painot) {
            s += "" + Arrays.toString(d) + "\n";
        }
        return s;
    }

    ///////////////////////////
    // AUTOMAATTISET METODIT //
    ///////////////////////////
    // lisätty GUI:ta varten

    public int getRivit() {
        return rivit;
    }


    public int getSarakkeet() {
        return sarakkeet;
    }

    public double[][] getPainot() {
        return painot;
    }

}
