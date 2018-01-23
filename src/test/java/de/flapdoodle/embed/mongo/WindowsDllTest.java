package de.flapdoodle.embed.mongo;

import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.distribution.BitSize;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WindowsDllTest {

    @Test
    public void testSingleVersion() throws IOException {

        RuntimeConfigBuilder defaultBuilder = new RuntimeConfigBuilder()
                .defaults(Command.MongoD);

        IRuntimeConfig config = defaultBuilder.build();

        check(config, new Distribution(Version.V3_6_0, Platform.Windows, BitSize.B64));
    }


    private void check(IRuntimeConfig runtime, Distribution distribution) throws IOException {
        assertTrue("Check", runtime.getArtifactStore().checkDistribution(distribution));
        IExtractedFileSet files = runtime.getArtifactStore().extractFileSet(distribution);
        assertThat(files.files(FileType.Library).size(), is(2));
        assertNotNull("Extracted", files);
        assertNotNull("Extracted", files.executable());
        assertTrue("Delete", files.executable().delete());
    }

}
