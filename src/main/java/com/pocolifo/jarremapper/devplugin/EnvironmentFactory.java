package com.pocolifo.jarremapper.devplugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentFactory {
	public abstract static class Environment {
		public File directory;
		public String minecraftVersion;

		public void setUp() throws IOException {
			Files.createDirectories(this.directory.toPath());
			this.download();
		}

		public void updateDirectories() {
			this.directory = new File("src/test/resources/" + this.getName() + "/"
					+ this.minecraftVersion);
		}

		protected boolean applies(String url, String minecraftVersion) throws IOException {
			String metadata = new String(IOUtility.download(new URL(url)));
			return metadata.contains(minecraftVersion);
		}

		public abstract void download() throws IOException;
		public abstract void parse(String[] fields);
		public abstract String getName();
		public abstract boolean appliesToVersion(String minecraftVersion) throws IOException;
	}

	public abstract static class Yarn extends Environment {
		public String buildId;

		private File regular;
		private File merged;

		@Override
		public void updateDirectories() {
			super.updateDirectories();

			this.regular = new File(this.directory, "regular.jar");
			this.merged = new File(this.directory, "merged.jar");
		}

		@Override
		public void download() throws IOException {


			IOUtility.download(new URL(this.getRegular()), this.regular,
					false);
			IOUtility.download(new URL(this.getMerged()), this.merged,
					false);

			IOUtility.extract(this.regular, s -> "mappings.tiny","mappings/mappings.tiny");
			IOUtility.extract(this.merged, s -> "mappings-merged.tiny","mappings/mappings.tiny");
		}

		public String getFullBuildId() {
			return this.minecraftVersion + "+build." + this.buildId;
		}

		public String getRegular() {
			return this.getRepoUrl() + "/net/fabricmc/yarn/" + this.getFullBuildId()
					+ "/yarn-" + this.getFullBuildId() + ".jar";
		}

		public String getMerged() {
			return this.getRepoUrl() + "/net/fabricmc/yarn/" + this.getFullBuildId()
					+ "/yarn-" + this.getFullBuildId() + "-mergedv2.jar";
		}

		abstract String getRepoUrl();
	}

	public static class FabricYarn extends Yarn {
		@Override
		String getRepoUrl() {
			return "https://maven.fabricmc.net";
		}

		@Override
		public void parse(String[] fields) {
			this.minecraftVersion = fields[1];
			this.buildId = fields[2];
			this.updateDirectories();
		}

		@Override
		public String getName() {
			return "fabric";
		}

		@Override
		public boolean appliesToVersion(String minecraftVersion) throws IOException {
			return this.applies("https://maven.fabricmc.net/net/fabricmc/yarn/versions.json", minecraftVersion);
		}
	}

	public static class LegacyYarn extends Yarn {
		@Override
		String getRepoUrl() {
			return "https://maven.legacyfabric.net";
		}

		@Override
		public void parse(String[] fields) {
			this.minecraftVersion = fields[1];
			this.buildId = fields[2];
			this.updateDirectories();
		}

		@Override
		public String getName() {
			return "legacyfabric";
		}

		@Override
		public boolean appliesToVersion(String minecraftVersion) throws IOException {
			return this.applies("https://maven.legacyfabric.net/net/fabricmc/yarn/maven-metadata.xml", minecraftVersion);
		}
	}

	public static class MCP extends Environment {
		public String channel;
		public String version;

		private File csv;
		private File srg;

		@Override
		public void updateDirectories() {
			super.updateDirectories();

			this.csv = new File(this.directory, "csv.zip");
			this.srg = new File(this.directory, "srg.zip");
		}

		@Override
		public void download() throws IOException {
			IOUtility.download(new URL(this.getCsvUrl()), this.csv,
					false);

			IOUtility.download(new URL(this.getSrgUrl()), this.srg,
					false);

			IOUtility.extract(this.csv, s -> s,"fields.csv", "methods.csv", "params.csv");
			IOUtility.extract(this.srg, s -> s,"joined.srg", "joined.exc");
		}

		@Override
		public void parse(String[] fields) {
			this.channel = fields[1];
			this.minecraftVersion = fields[2];
			this.version = fields[3];
			this.updateDirectories();
		}

		@Override
		public String getName() {
			return "mcp";
		}

		@Override
		public boolean appliesToVersion(String minecraftVersion) throws IOException {
			return this.applies("https://maven.minecraftforge.net/de/oceanlabs/mcp/versions.json", minecraftVersion);
		}

		public String getCsvUrl() {
			return "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_" + this.channel + "/" + this.version + "-" +
					this.minecraftVersion + "/mcp_" + this.channel +"-" + this.version + "-" + this.minecraftVersion +
					".zip";
		}

		public String getSrgUrl() {
			return "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/" + this.minecraftVersion + "/mcp-" +
					this.minecraftVersion + "-srg.zip";
		}
	}

	public static List<Environment> getInstance(JarRemapperExtension extension) {
		List<Environment> environments = new ArrayList<>();

		for (String version : extension.mappings) {
			String[] fields = version.split("/");

			switch (fields[0].toLowerCase()) {
				case "fabric": {
					Environment env = new FabricYarn();
					env.parse(version.split("/"));
					environments.add(env);
					break;
				}

				case "legacyfabric": {
					Environment env = new LegacyYarn();
					env.parse(version.split("/"));
					environments.add(env);
					break;
				}

				case "mcp": {
					Environment env = new MCP();
					env.parse(version.split("/"));
					environments.add(env);
					break;
				}
			}
		}

		return environments;
	}
}
