/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EsgDetailedTwoWindingTransformer {

    public enum RegulatingMode {
        NOT_REGULATING,
        VOLTAGE,
        ACTIVE_FLUX_SIDE_1,
        ACTIVE_FLUX_SIDE_2
    }

    public static class Tap {
        private final int iplo; // tap number
        private final double uno1; // sending side voltage [kV]
        private final double uno2; // receiving side voltage [kV]
        private final double ucc; // leakage impedance [%]
        private final double dephas; // phase shift angle [deg]

        public Tap(int iplo, double dephas, double uno1, double uno2, double ucc) {
            this.iplo = iplo;
            this.dephas = dephas;
            this.uno1 = uno1;
            this.uno2 = uno2;
            this.ucc = ucc;
        }

        public double getDephas() {
            return dephas;
        }

        public int getIplo() {
            return iplo;
        }

        public double getUcc() {
            return ucc;
        }

        public double getUno1() {
            return uno1;
        }

        public double getUno2() {
            return uno2;
        }
    }

    private final EsgBranchName name;
    private final EsgBranchConnectionStatus status;
    private final double rate; // rated apparent power [MVA]
    private final double pcu; // Cu losses [% base RATE]
    private final double pfer; // Iron losses [% base RATE]
    private final double cmagn; // magnetizing current [%]
    private final double esat; // saturation exponent

    private final int ktpnom; // nominal tap number
    private final int ktap8; // initial tap position (tap number)
    private final Esg8charName zbusr; // regulated node name (if empty, no tap change)
    private double voltr; // voltage target [kV]
    private final double pregmin; // min active flux [MW]
    private final double pregmax; //  max active flux [MW]
    private final RegulatingMode xregtr; // regulating mode

    private final List<Tap> taps = new ArrayList<>(1);

    public EsgDetailedTwoWindingTransformer(EsgBranchName name, EsgBranchConnectionStatus status, double cmagn,
                                            double rate, double pcu, double pfer, double esat, int ktpnom, int ktap8, Esg8charName zbusr,
                                            double voltr, double pregmin, double pregmax, RegulatingMode xregtr) {
        this.name = Objects.requireNonNull(name);
        this.status = status;
        this.cmagn = cmagn;
        this.rate = rate;
        this.pcu = pcu;
        this.pfer = pfer;
        this.esat = esat;
        this.ktpnom = ktpnom;
        this.ktap8 = ktap8;
        this.zbusr = zbusr;
        this.voltr = voltr;
        this.pregmin = pregmin;
        this.pregmax = pregmax;
        this.xregtr = Objects.requireNonNull(xregtr);
    }

    public EsgBranchName getName() {
        return name;
    }

    public EsgBranchConnectionStatus getStatus() {
        return status;
    }

    public double getCmagn() {
        return cmagn;
    }

    public double getEsat() {
        return esat;
    }

    public int getKtap8() {
        return ktap8;
    }

    public int getKtpnom() {
        return ktpnom;
    }

    public double getPcu() {
        return pcu;
    }

    public double getPfer() {
        return pfer;
    }

    public double getPregmax() {
        return pregmax;
    }

    public double getPregmin() {
        return pregmin;
    }

    public double getRate() {
        return rate;
    }

    public List<Tap> getTaps() {
        return taps;
    }

    public double getVoltr() {
        return voltr;
    }

    public void setVoltr(double voltr) {
        this.voltr = voltr;
    }

    public RegulatingMode getXregtr() {
        return xregtr;
    }

    public Esg8charName getZbusr() {
        return zbusr;
    }
}
