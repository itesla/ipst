package eu.itesla_project.iidm.ddb.eurostag;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class EurostagDDBTest {

    @Test
    public void testEurostagDDB() throws IOException, URISyntaxException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
        Path path1 = fs.getPath("/path1");
        Path file1 = fs.getPath("/path1/file1.tg");
        Path file2 = fs.getPath("/path1/.tg");
        Path lnpath3 = fs.getPath("/path1/lnpath3");
        Path ln  = fs.getPath("/path1/ln.tg");
        Path path4  = fs.getPath("/path1/path4");
        Path file4  = fs.getPath("/path1/path4/file4.tg");
        Path path2 = fs.getPath("/path2");
        Path filereal = fs.getPath("/path2/filereal.tg");
        Path path3 = fs.getPath("/path3");
        Path p3file = fs.getPath("/path3/p3file.tg");
        Files.createDirectory(path1);
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createSymbolicLink(lnpath3, path3);
        Files.createSymbolicLink(ln, filereal);
        Files.createDirectory(path4);
        Files.createFile(file4);
        Files.createDirectory(path2);
        Files.createFile(filereal);
        Files.createDirectory(path3);
        Files.createFile(p3file);
        List<Path> ddbDirs = new ArrayList<Path>();
        ddbDirs.add(path1);
        EurostagDDB eurostagDDB = new EurostagDDB(ddbDirs);
        assertEquals(eurostagDDB.findGenerator("").getFileName().toString(), ".tg");
        assertEquals(eurostagDDB.findGenerator("p3file").getFileName().toString(), "p3file.tg");
        assertEquals(eurostagDDB.findGenerator("file4").getFileName().toString(), "file4.tg");
        assertEquals(eurostagDDB.findGenerator("file1").getFileName().toString(), "file1.tg");
        assertEquals(eurostagDDB.findGenerator("filereal").getFileName().toString(), "filereal.tg");
        }
    }

    @Test
    public void testFindGenerator() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
        Path path2 = fs.getPath("/path2");
        Path filereal = fs.getPath("/path2/filereal.tg");
        Path path = fs.getPath("lnpath2");
        Files.createDirectory(path2);
        Files.createFile(filereal);
        Files.createSymbolicLink(path, path2);
        List<Path> ddbDirs = new ArrayList<Path>();
        ddbDirs.add(path);
        EurostagDDB eurostagDDB = new EurostagDDB(ddbDirs);
        assertEquals(eurostagDDB.findGenerator("filereal").getFileName().toString(), "filereal.tg");
        }
    }

}
