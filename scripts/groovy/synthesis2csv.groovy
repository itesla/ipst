/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import groovy.json.JsonSlurper
import com.powsybl.commons.io.table.TableFormatterConfig
import com.powsybl.commons.io.table.CsvTableFormatterFactory
import com.powsybl.commons.io.table.Column
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


if (args.size() < 2) {
    println "Usage: itools run-script --file synthesis2csv.groovy <geoCodesFile> <jsonFile>"
    return
}

def fpath = args[1]
def geoname = args[0]


def areaMap = [:]
def geomapFile = new File(geoname)
String[] lines = geomapFile.text.split('\n')
lines.each { def vals = it.split(';'); areaMap.put(vals[0], vals[1].trim()) }


def inputFile = new File(fpath)
def filename = inputFile.getName();
def data = new JsonSlurper().parseText(inputFile.text)

def outFile = data.processId + ".csv"



def columns = ["State", "Contingency", "Type", "Name", "Limit (A, kV)", "Equipment", "Area", "Nominal Voltage (kV)", "Worst Relative Value (%)", "Worst Relative Value Timestamp"]
def column_keys = ["state", "contingency", "type", "name", "limit", "equipment", "area", "voltageLevel", "worstRelativeValue", "worstRelativeTime"]

def timestamps = []
def rows = []

data.states.each { st ->
    st.preContingencyViolations.each {
        rows.add(getRowMap(timestamps, areaMap, st.state, 'N_State', it))
    }

    st.postContingencyViolations.each { cont ->
        cont.value.each { v ->

            rows.add(getRowMap(timestamps, areaMap, st.state, cont.key, v))
        }
    }
}

timestamps.sort()

def writer = Files.newBufferedWriter(Paths.get(outFile), StandardCharsets.UTF_8)

def tablecolumns = []
columns.each { c -> tablecolumns.push(new Column(c)) }
timestamps.each { t -> tablecolumns.push(new Column(t)) }

TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load()
def formatterFactory = new CsvTableFormatterFactory()
def formatter = formatterFactory.create(writer, "#" + filename, tableFormatterConfig, tablecolumns.toArray(new Column[tablecolumns.size()]))


rows.each { row ->
    column_keys.each { formatter.writeCell(row[it] != null ? row[it] : "") }
    timestamps.each { formatter.writeCell(row["vals"][it] != null ? row["vals"][it] : "") }
}

writer.close()


def getRowMap(timestamps, areaMap, state, k, v) {

    def rowMap = [:]
    rowMap["contingency"] = k
    rowMap["state"] = state
    rowMap["type"] = v.type
    rowMap["name"] = v.limitName
    rowMap["limit"] = v.limit
    rowMap["equipment"] = v.equipment
    if (v.type == 'CURRENT') {
        rowMap["area"] = [areaMap[v.equipment.substring(0, 5)], areaMap[v.equipment.substring(v.equipment.length() - 5)]].unique().sort().join(' ')
    } else
        rowMap["area"] = areaMap[v.equipment.substring(0, 5)]
    rowMap["voltageLevel"] = v.voltageLevel
    def vals = [:]
    def worst
    def tworst = ""
    v.timeValues.each { tv ->
        if (!timestamps.contains(tv.timestamp)) {
            timestamps.add(tv.timestamp)
        }
        tv.indicators.each { ind ->
            if (ind.id == "ABSOLUTE") {
                vals[tv.timestamp] = ind.value
                if (worst == null || ((v.type == 'LOW_VOLTAGE') ? (ind.value < worst) : (ind.value > worst))) {
                    worst = ind.value
                    tworst = tv.timestamp
                }
            }
        }
    }
    rowMap["vals"] = vals
    rowMap["worstRelativeValue"] = worst ? (worst / v.limit) : null
    rowMap["worstRelativeTime"] = tworst
    return rowMap
}
