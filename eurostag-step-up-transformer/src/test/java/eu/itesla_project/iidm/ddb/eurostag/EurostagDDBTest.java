package eu.itesla_project.iidm.ddb.eurostag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class EurostagDDBTest {

    @Test
    public void testEurostagDDB() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path folder1 = Files.createDirectory(fs.getPath("/folder1"));
            Path folder2 = Files.createDirectory(fs.getPath("/folder2"));
            Path folder3 = Files.createDirectory(fs.getPath("/folder3"));

            Path generator1 = Files.createFile(folder1.resolve("generator1.tg"));
            Path generator2 = Files.createFile(folder1.resolve("generator2.tg"));

            Files.createFile(folder2.resolve("generator3.tg.bck"));
            Path link = Files.createSymbolicLink(folder2.resolve("folder3"), folder3);

            Path generator4 = Files.createFile(link.resolve("generator4.tg"));
            Path dataFile = Files.createFile(folder3.resolve("generator.data"));
            Files.createSymbolicLink(folder3.resolve("generator5.tg"), dataFile);

            EurostagDDB eurostagDDB = new EurostagDDB(Arrays.asList(folder1, folder2));
            assertEquals(generator1, eurostagDDB.findGenerator("generator1"));
            assertEquals(generator2, eurostagDDB.findGenerator("generator2"));
            assertNull(eurostagDDB.findGenerator("generator3"));
            assertEquals(generator4, eurostagDDB.findGenerator("generator4"));
            assertEquals(dataFile, eurostagDDB.findGenerator("generator5"));
        }
    }

    @Test
    public void testLinkFiles() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path folder = Files.createDirectory(fs.getPath("/tmp"));
            Path dataFile = Files.createFile(folder.resolve("generator.data"));
            Files.createSymbolicLink(folder.resolve("generator1.tg"), dataFile);
            Files.createSymbolicLink(folder.resolve("generator2.tg"), dataFile);
            Files.createSymbolicLink(folder.resolve("generator3.tg"), dataFile);
            Path generator4 = Files.createFile(folder.resolve("generator4.tg"));

            EurostagDDB eurostagDDB = new EurostagDDB(Collections.singletonList(folder));
            assertEquals(dataFile, eurostagDDB.findGenerator("generator1"));
            assertEquals(dataFile, eurostagDDB.findGenerator("generator2"));
            assertEquals(dataFile, eurostagDDB.findGenerator("generator3"));
            assertEquals(generator4, eurostagDDB.findGenerator("generator4"));
        }
    }

    @Test
    public void testLinkDirectory() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path folder = Files.createDirectory(fs.getPath("/folder"));
            Path file = Files.createFile(folder.resolve("generator.tg"));
            Path linkFolder = Files.createSymbolicLink(fs.getPath("/work/folder.link"), folder);

            EurostagDDB eurostagDDB = new EurostagDDB(Collections.singletonList(linkFolder));
            assertEquals(file, eurostagDDB.findGenerator("generator"));
        }
    }

}
