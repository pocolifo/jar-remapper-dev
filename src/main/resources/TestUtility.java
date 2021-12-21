/*
 * This is an automatically generated test class.
 * The next time you run the 'generateTests' task, this class will be overwritten.
 */

package %s;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.pocolifo.jarremapper.JarRemapper;
import com.pocolifo.jarremapper.mapping.JarMapping;
import com.pocolifo.jarremapper.engine.AbstractRemappingEngine;

public class TestUtility {
	public static final File DEFAULT_OUTPUT_FILE = new File("output.jar");

	public static File getResourceAsFile(String resource) {
		try {
			return new File(ClassLoader.getSystemResource(resource).toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void remap(JarMapping mapping, AbstractRemappingEngine engine, File input) throws IOException {
		remap(mapping, engine, input, DEFAULT_OUTPUT_FILE, true);
	}

	public static void remap(JarMapping mapping, AbstractRemappingEngine engine, File input, File output, boolean overwrite) throws IOException {
		JarRemapper remapper = JarRemapper.newRemap()
				.withRemappingEngine(engine)
				.withMappings(mapping)
				.withInputFile(input)
				.withOutputFile(output);

		if (overwrite) remapper.overwriteOutputFile();

		remapper.remap();
	}
}
