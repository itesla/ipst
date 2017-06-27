/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EsgSpecialParameters {

    public static final int DEFAULT_INPVPQ = 2;
    public static final float DEFAULT_THMAX = 0.1f;
    public static final float DEFAULT_EMAXF = 0.1f;
    public static final float DEFAULT_ZMIN = 0.0002f;
    public static final float DEFAULT_RAMIN = 0.8f;
    public static final float DEFAULT_RAMAX = 1.2f;
    public static final float DEFAULT_TOLPLO = 0.001f;

    public static final float ZMIN_HIGH = DEFAULT_ZMIN;
    public static final float ZMIN_LOW = 0.00005f;

    /**
     * Iteration number at which equipments are switched to/from upper/lower bounds
     */
    private int inpvpq;

    /**
     * Maximum voltage angle correction [degrees]
     */
    private float thmax;

    /**
     * Maximum voltage correction 0.1 [p.u.]
     */
    private float emaxf;

    /**
     * Minimum line impedance value [p.u.]  , default eurostag is 0.0002  (=4*0.00005)
     */
    private float zmin;

    /**
     * Minimum transformer ratio
     */
    private float ramin;

    /**
     * Maximum transformer ratio
     */
    private float ramax;

    /**
     * Accuracy on required voltage controlled by transformer [p.u.]
     */
    private float tolplo;

    public EsgSpecialParameters(int inpvpq, float thmax, float emaxf, float zmin, float ramin, float ramax, float tolplo) {
        this.inpvpq = inpvpq;
        this.thmax = thmax;
        this.emaxf = emaxf;
        this.zmin = zmin;
        this.ramin = ramin;
        this.ramax = ramax;
        this.tolplo = tolplo;
    }

    public EsgSpecialParameters() {
        this(DEFAULT_INPVPQ, DEFAULT_THMAX, DEFAULT_EMAXF, DEFAULT_ZMIN, DEFAULT_RAMIN, DEFAULT_RAMAX, DEFAULT_TOLPLO);
    }

    public int getInpvpq() {
        return inpvpq;
    }

    public void setInpvpq(int inpvpq) {
        this.inpvpq = inpvpq;
    }

    public float getThmax() {
        return thmax;
    }

    public void setThmax(float thmax) {
        this.thmax = thmax;
    }

    public float getEmaxf() {
        return emaxf;
    }

    public void setEmaxf(float emaxf) {
        this.emaxf = emaxf;
    }

    public float getZmin() {
        return zmin;
    }

    public void setZmin(float zmin) {
        this.zmin = zmin;
    }

    public float getRamin() {
        return ramin;
    }

    public void setRamin(float ramin) {
        this.ramin = ramin;
    }

    public float getRamax() {
        return ramax;
    }

    public void setRamax(float ramax) {
        this.ramax = ramax;
    }

    public float getTolplo() {
        return tolplo;
    }

    public void setTolplo(float tolplo) {
        this.tolplo = tolplo;
    }
}
