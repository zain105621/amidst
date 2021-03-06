package amidst.settings.biomeprofile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import amidst.documentation.GsonConstructor;
import amidst.documentation.Immutable;
import amidst.logging.AmidstLogger;
import amidst.mojangapi.world.biome.Biome;
import amidst.mojangapi.world.biome.BiomeColor;

@Immutable
public class BiomeProfile {
	private static Map<String, BiomeColorJson> createDefaultColorMap() {
		Map<String, BiomeColorJson> result = new HashMap<>();
		for (Biome biome : Biome.allBiomes()) {
			result.put(biome.getName(), biome.getDefaultColor().createBiomeColorJson());
		}
		return result;
	}

	public static BiomeProfile getDefaultProfile() {
		return DEFAULT_PROFILE;
	}

	private static final BiomeProfile DEFAULT_PROFILE = new BiomeProfile("default", null, createDefaultColorMap());

	private volatile String name;
	private volatile String shortcut;
	private volatile Map<String, BiomeColorJson> colorMap;

	@GsonConstructor
	public BiomeProfile() {
	}

	private BiomeProfile(String name, String shortcut, Map<String, BiomeColorJson> colorMap) {
		this.name = name;
		this.shortcut = shortcut;
		this.colorMap = colorMap;
	}

	public String getName() {
		return name == null ? "<unnamed>" : name;
	}

	public String getShortcut() {
		return shortcut;
	}

	public boolean validate() {
		if(this.colorMap == null) {
			AmidstLogger.info("Color map is missing in profile: {}", name);
			return false;
		}

		if(this.name == null) {
			AmidstLogger.info("Name is missing in profile");
			return false;
		}

		for (String biomeName : colorMap.keySet()) {
			if (!Biome.exists(biomeName)) {
				AmidstLogger.info("Failed to find biome for: {} in profile: {}", biomeName, name);
			}
		}
		return true;
	}

	public BiomeColor[] createBiomeColorArray() {
		BiomeColor[] result = new BiomeColor[Biome.getBiomesLength()];
		for (Biome biome : Biome.allBiomes()) {
			result[biome.getIndex()] = getBiomeColor(biome);
		}
		return result;
	}

	private BiomeColor getBiomeColor(Biome biome) {
		if (colorMap != null && colorMap.containsKey(biome.getName())) {
			return colorMap.get(biome.getName()).createBiomeColor();
		} else {
			return biome.getDefaultColor();
		}
	}

	public boolean save(Path file) {
		return writeToFile(file, serialize());
	}

	private String serialize() {
		String output = "{ \"name\":\"" + name + "\", \"colorMap\":[\r\n";
		output += serializeColorMap();
		return output + " ] }\r\n";
	}

	/**
	 * This method uses the sorted color map, so the serialization will have a
	 * reproducible order.
	 */
	private String serializeColorMap() {
		String output = "";
		for (Map.Entry<String, BiomeColorJson> pairs : getSortedColorMapEntries()) {
			output += "[ \"" + pairs.getKey() + "\", { ";
			output += "\"r\":" + pairs.getValue().getR() + ", ";
			output += "\"g\":" + pairs.getValue().getG() + ", ";
			output += "\"b\":" + pairs.getValue().getB() + " } ],\r\n";
		}
		return output.substring(0, output.length() - 3);
	}

	private Set<Entry<String, BiomeColorJson>> getSortedColorMapEntries() {
		if(colorMap == null) {
			return Collections.emptySet();
		}

		SortedMap<String, BiomeColorJson> result = new TreeMap<>(Biome::compareByIndex);
		result.putAll(colorMap);
		return result.entrySet();
	}

	private boolean writeToFile(Path file, String output) {
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			writer.write(output);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
