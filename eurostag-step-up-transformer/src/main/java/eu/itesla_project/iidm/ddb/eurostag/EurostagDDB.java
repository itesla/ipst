/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class EurostagDDB {

    private final Map<String, Path> generators = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagDDB.class);
    EurostagDDB(List<Path> ddbDirs) throws IOException {
        for (Path ddbDir : ddbDirs) {
            if (Files.isSymbolicLink(ddbDir)) {
                ddbDir = Files.readSymbolicLink(ddbDir);
            }
            if (!Files.exists(ddbDir) && !Files.isDirectory(ddbDir)) {
                throw new IllegalArgumentException(ddbDir + " must exist and be a dir");
            }
            Files.walkFileTree(ddbDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    Path tmpfile = file;
                    if (Files.isSymbolicLink(tmpfile)) {
                        tmpfile = Files.readSymbolicLink(file);
                        while (Files.isSymbolicLink(tmpfile)) {
                            tmpfile = Files.readSymbolicLink(tmpfile);
                        }
                        if (Files.isDirectory(tmpfile)) {
                            Files.walkFileTree(tmpfile, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, this);
                        }
                    }
                    if (Files.isRegularFile(tmpfile) && tmpfile.toString().endsWith(".tg")) {
                        String key = fileName.substring(0, fileName.length() - 3);
                        if (generators.containsKey(key)) {
                            LOGGER.error("the processing has detected that the file " + fileName + " is present in " + tmpfile + " and " + generators.get(key));
                        }
                        generators.put(fileName.substring(0, fileName.length() - 3), tmpfile);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    Path findGenerator(String idDdb) throws IOException {
        return generators.get(idDdb);
    }

}
