package eu.itesla_project.iidm.ddb.eurostag;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class EurostagDDBTest {

    @Test
    public void testEurostagDDB() throws IOException, URISyntaxException {
        Path path1 = Paths.get("testsymboliclink/path1");
        List<Path> ddbDirs = new ArrayList<Path>();
        ddbDirs.add(path1);
        EurostagDDB eurostagDDB = new EurostagDDB(ddbDirs);
        assertEquals(eurostagDDB.findGenerator("").getFileName().toString(), ".tg");
        assertEquals(eurostagDDB.findGenerator("p3file").getFileName().toString(), "p3file.tg");
        assertEquals(eurostagDDB.findGenerator("file4").getFileName().toString(), "file4.tg");
        assertEquals(eurostagDDB.findGenerator("file1").getFileName().toString(), "file1.tg");
        assertEquals(eurostagDDB.findGenerator("filereal").getFileName().toString(), "filereal.tg");
    }

    @Test
    public void testFindGenerator() throws IOException {
        Path path = Paths.get("testsymboliclink/lnpath2");
        List<Path> ddbDirs = new ArrayList<Path>();
        ddbDirs.add(path);
        EurostagDDB eurostagDDB = new EurostagDDB(ddbDirs);
        assertEquals(eurostagDDB.findGenerator("filereal").getFileName().toString(), "filereal.tg");
    }

}
