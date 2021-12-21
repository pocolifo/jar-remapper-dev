package com.pocolifo.jarremapper.devplugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.pocolifo.jarremapper.devplugin.tasks.GenerateTestsTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JarRemapperDevPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getLogger().lifecycle("JARRemapperDev 1.0-SNAPSHOT");

		project.getExtensions().create("jarremapper", JarRemapperExtension.class);

		project.getTasks().register("generateTests", GenerateTestsTask.class);

		project.afterEvaluate(p -> {
			JarRemapperExtension extension =
					Objects.requireNonNull(p.getExtensions().findByType(JarRemapperExtension.class));

			if (extension.mappings == null || extension.minecraft == null) {
				p.getLogger().warn("[JARRemapperDev] Mappings and Minecraft binaries can only be downloaded " +
						"when set in the extension");

				return;
			}

			try {
				this.downloadMappings(extension);
			} catch (IOException e) {
				p.getLogger().error("Could not download mappings!", e);
			}

			try {
				this.downloadMinecraftBinaries(extension);
			} catch (IOException e) {
				p.getLogger().error("Could not download Minecraft binaries!", e);
			}
		});
	}

	private void downloadMinecraftBinaries(JarRemapperExtension extension) throws IOException {
		for (String minecraft : extension.minecraft) {
			String[] fields = minecraft.split("/");

			String side = fields[0];
			String version = fields[1];
			String hash = fields[2];

			IOUtility.download(
					new URL("https://launcher.mojang.com/v1/objects/" + hash + "/" + side + ".jar"),
					new File("src/test/resources/minecraft-" + version + ".jar"),
					false
			);
		}
	}

	private void downloadMappings(JarRemapperExtension extension) throws IOException {
		for (EnvironmentFactory.Environment environment : EnvironmentFactory.getInstance(extension)) {
			environment.setUp();
		}
	}
}
