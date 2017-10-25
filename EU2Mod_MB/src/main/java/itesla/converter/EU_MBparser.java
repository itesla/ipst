/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package itesla.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to parse macroblocks in Eurostag
 * @author Marc Sabate <sabatem@aia.es>
 * @author Raul Viruez <viruezr@aia.es>
 */
public class EU_MBparser {
    private File EUfile;
    private String[][] paramEu;
    private String[][] entries;
    private String[] Blocksoutput;
    private Integer[] GraphicalNumber;
    private Integer[] idEu;
    private Integer[][] link;
    private Integer nLinks;
    private Integer nBlocks;

    /*
     * Class to parse an *.frm/*.fri files. It saves the following information regarding the macroblocks:
     * EUfile: path of the file that will be parsed.
     * nBlocks: number of blocks inside the macroblock.
     * paramEu: an 8 x nBlocks array. Each mathematic block has a maximum of 8 parameters: 6 parameters, offset and init_value.
     * entries: an 5 x nBlocks array. Each mathematic block has 5 input pins.
     * Blocksoutput: nBlocks array. Each block has 1 output pin.
     * GraphicalNumber: nBLocks array. Each instance of a block has an graphical id
     * idEu: nBlocks array. Each type of a block has an Eurostag id.
     * nLinks: number of links.
     * link: nLinks x 3 array. Each row is a link: the output block and the input block.
     */
    public EU_MBparser(File euFile) {
        this.EUfile = euFile;
        try {
            parser();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parser() throws IOException {
        String sep = "\\s+";
        String line;
        List<String[]> dataFile = new ArrayList<String[]>();
        String[] dataLine;
        BufferedReader buffer;
        buffer = new BufferedReader(new FileReader(EUfile));

        while ((line = buffer.readLine()) != null) {
            dataLine = line.trim().split(sep);
            dataFile.add(dataLine);
        }
        buffer.close();

        //parse
        nBlocks = Integer.parseInt(dataFile.get(2)[0]);
        if (nBlocks > 0) {
            Integer rowsPerBlockPars = (nBlocks - 1) / 6 + 1;
            Integer rowsPerBlockEntries = (nBlocks - 1) / 14 + 1;
            Integer rowsPerBlockOutput = (nBlocks - 1) / 13 + 1;
            Integer rowsPerBlockGraph = (nBlocks - 1) / 14 + 1;
            Integer rowsPerBlockLocation = (nBlocks - 1) / 21 + 1;
            Integer rowsPerBlockType = (nBlocks - 1) / 21 + 1;

            paramEu = new String[8][nBlocks];
            entries = new String[5][nBlocks];
            Blocksoutput = new String[nBlocks];
            GraphicalNumber = new Integer[nBlocks];
            idEu = new Integer[nBlocks];

            //parameters
            int rowIni = 3;
            int nDatosPerBlock = 8;
            String[][] auxParameters = Extract(dataFile, rowIni, rowsPerBlockPars, nDatosPerBlock, nBlocks);
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < nBlocks; j++) {
                    paramEu[i][j] = auxParameters[i][j];
                }
            }

            //entries
            rowIni = rowIni + nDatosPerBlock * rowsPerBlockPars;
            nDatosPerBlock = 5;
            String[][] auxEntries = Extract(dataFile, rowIni, rowsPerBlockEntries, nDatosPerBlock, nBlocks);
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < nBlocks; j++) {
                    entries[i][j] = auxEntries[i][j];
                }
            }

            //output
            rowIni = rowIni + nDatosPerBlock * rowsPerBlockEntries;
            nDatosPerBlock = 1;
            String[][] auxOutput = Extract(dataFile, rowIni, rowsPerBlockOutput, nDatosPerBlock, nBlocks);
            for (int j = 0; j < nBlocks; j++) {
                Blocksoutput[j] = auxOutput[0][j];
            }

            //graphical number
            rowIni = rowIni + nDatosPerBlock * rowsPerBlockOutput;
            nDatosPerBlock = 1;
            String[][] auxGraphical = Extract(dataFile, rowIni, rowsPerBlockGraph, nDatosPerBlock, nBlocks);
            for (int j = 0; j < nBlocks; j++) {
                GraphicalNumber[j] = Integer.parseInt(auxGraphical[0][j]);
            }

            //id Eurostag
            rowIni = rowIni + rowsPerBlockGraph + 2 * rowsPerBlockLocation;
            nDatosPerBlock = 1;
            String[][] auxIdEu = Extract(dataFile, rowIni, rowsPerBlockType, nDatosPerBlock, nBlocks);
            for (int j = 0; j < nBlocks; j++) {
                idEu[j] = Integer.parseInt(auxIdEu[0][j]);
            }

            //Links
            rowIni = rowIni + 3 * rowsPerBlockType * nDatosPerBlock;
            nLinks = Integer.parseInt(dataFile.get(rowIni)[0]);
            Integer rowsPerLink = (nLinks - 1) / 21 + 1;

            link = new Integer[nLinks][3];
            rowIni = rowIni + 22 * rowsPerLink + 1;
            if (nLinks > 0) {
                String[][] auxLink = Extract(dataFile, rowIni, rowsPerLink, 3, nLinks);
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < nLinks; j++) {
                        link[j][i] = Integer.parseInt(auxLink[i][j]);
                    }
                }
            }
        }
    }

    public String[][] getParamEU() {
        return paramEu;
    }

    public String[][] getEntries() {
        return entries;
    }

    public String[] getBlocksoutput() {
        return Blocksoutput;
    }

    public Integer[] getGraphicalNumber() {
        return GraphicalNumber;
    }

    public Integer[] getIdEu() {
        return idEu;
    }

    public Integer[][] getLink() {
        return link;
    }

    public Integer getnBlocks() {
        return nBlocks;
    }

    public Integer getnLinks() {
        return nLinks;
    }

    static private String[][] Extract(List<String[]> frmFile, Integer ini, Integer rowsPerBlockPars, Integer nDatosPerBlock, Integer nBlocks) {
        String[][] output = new String[nDatosPerBlock][nBlocks];
        Integer count = 0;
        Integer indexBlock;
        Integer indexValue;
        Integer nlines = rowsPerBlockPars * nDatosPerBlock;
        for (int i = 0; i < nlines; ++i) {
            for (int j = 0; j < frmFile.get(ini + i).length; j++) {
                indexBlock = count % nBlocks;
                indexValue = count / nBlocks;
                output[indexValue][indexBlock] = frmFile.get(ini + i)[j];
                count += 1;
            }
        }
        return output;
    }
}
