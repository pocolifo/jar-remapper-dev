package com.pocolifo.jarremapper.devplugin.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pocolifo.jarremapper.devplugin.EnvironmentFactory;
import com.pocolifo.jarremapper.devplugin.IOUtility;
import com.pocolifo.jarremapper.devplugin.JarRemapperExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class GenerateTestsTask extends DefaultTask {
	public GenerateTestsTask() {
		this.setGroup("jarremapperdev");
		this.setDescription("Generates test classes");
	}

	@TaskAction
	public void generateTests() throws IOException {
		Project project = this.getProject();
		JarRemapperExtension extension = this.getProject().getExtensions().getByType(JarRemapperExtension.class);

		if (extension.readers == null || extension.engines == null) {
			project.getLogger().warn("[JARRemapperDev] Tests can only be generated when 'readers' and 'engines' is " +
					"set in the extension.");
			return;
		}

		String testPackage = project.getGroup() + "." + project.getName().replace("-", "")
				+ ".tests";
		File testDirectory = new File("src/test/java/" + testPackage.replaceAll("\\.", "/"));
		Files.createDirectories(testDirectory.toPath());

		String testUtility = new String(IOUtility.readResource("TestUtility.java"));
		testUtility = String.format(testUtility, testPackage);
		Files.write(new File(testDirectory, "TestUtility.java").toPath(),
				testUtility.getBytes(StandardCharsets.UTF_8));
		testUtility = null;

		String baseTest = new String(IOUtility.readResource("BaseTest.java"));
		String baseTestMethod = new String(IOUtility.readResource("BaseTestMethod.java"));

		List<String> imports = new ArrayList<>();
		Map<String, List<String>> versionTestMethodsMap = new HashMap<>();

		int uniqueId = 0;

		for (EnvironmentFactory.Environment environment : EnvironmentFactory.getInstance(extension)) {
			if (!environment.appliesToVersion(environment.minecraftVersion)) continue;
			versionTestMethodsMap.putIfAbsent(environment.minecraftVersion, new ArrayList<>());

			for (Map.Entry<String, List<String>> reader : extension.readers.entrySet()) {
				for (String engine : extension.engines) {
					// engine
					String[] engineFields = engine.split("/");
					String engineImport = engineFields[0];
					String engineConstructor = engineFields[1];
					String engineName = IOUtility.getFileName(engineImport.replaceAll("\\.", "/"));

					// reader
					String[] fields = reader.getKey().split("/");
					String fullClass = fields[0];
					String readMethod = fields[1];
					String[] appliesToEnvironments = fields[2].split("\\+");
					String className = IOUtility.getFileName(fullClass.replaceAll("\\.", "/"));
					File readingDirectory = new File(environment.directory.toString().replace(
							"src/test/resources/", ""));

					// check if this test can be created
					boolean applies = false;

					for (String appliesToEnvironment : appliesToEnvironments) {
						if (environment.getName().equals(appliesToEnvironment)) {
							applies = true;
							break;
						}
					}

					if (!applies) continue;

					// create & add the test method
					StringBuilder testUtilityCall = new StringBuilder("TestUtility.remap(");

					imports.add(fullClass);
					imports.add(engineImport);

					StringBuilder constructor = new StringBuilder("new ").append(className).append("(");

					for (String fileName : reader.getValue()) {
						constructor.append("TestUtility.getResourceAsFile(\"").append(new File(readingDirectory,
								fileName)).append("\"), ");
					}

					int length = constructor.length();
					constructor.delete(length - 2, length);
					constructor.append(")").append(readMethod);

					testUtilityCall.append(constructor).append(", ").append(engineConstructor)
							.append(", TestUtility.getResourceAsFile(\"minecraft-")
							.append(environment.minecraftVersion).append(".jar\"));");

					String testName = "test" + className + "$mc" +
							environment.minecraftVersion.replaceAll("\\.", "_") + "$" + environment.getName() +
							"$" + engineName + "$" + uniqueId;

					versionTestMethodsMap.get(
							environment.minecraftVersion
					).add(String.format(baseTestMethod, testName, testUtilityCall));

					uniqueId++;
				}
			}
		}

		// add imports
		StringBuilder importString = new StringBuilder();

		for (String anImport : imports) {
			if (!importString.toString().contains(anImport)) {
				importString.append("import ").append(anImport).append(";\n");
			}
		}

		for (Map.Entry<String, List<String>> entry : versionTestMethodsMap.entrySet()) {
			// format the entire class
			String newClassName = entry.getKey().replaceAll("\\.", "_");

			String testClass = String.format(baseTest, testPackage, importString,
					newClassName,
					String.join("\n\n", versionTestMethodsMap.get(entry.getKey())));

			// write out the entire class
			Files.write(new File(testDirectory, "TestMinecraft" + newClassName + ".java").toPath(),
					testClass.getBytes(StandardCharsets.UTF_8));
		}
	}
}
