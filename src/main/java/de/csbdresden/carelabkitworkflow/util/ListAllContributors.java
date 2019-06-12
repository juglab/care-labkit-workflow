package de.csbdresden.carelabkitworkflow.util;

import mdbtools.libmdb.file;
import net.imagej.updater.FilesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ListAllContributors {

	static String githubApi = "https://api.github.com";

	private static String getNameFromLogin(String login) {
		try {
			URL userUrl = new URL(githubApi + "/users/" + login);
			JSONObject userObj = new JSONObject(fromURL(userUrl));
			if(userObj == null || userObj.isNull("name")) return login;
			return userObj.getString("name");
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String getGithubUrl(String jarName, String version) {

		URL pomUrl = getPomUrl(jarName, version);
		if(pomUrl == null) return null;
		String output = null;
		try {
			output = fromURL(pomUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(output == null) {
			System.out.println("Could not read " + pomUrl);
			return null;
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(output)));

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList scm = doc.getElementsByTagName("scm");
			if(scm == null || scm.getLength() == 0) {
//				System.out.println("no scm tag found");
				return null;
			}
			NodeList url = ((Element) scm.item(0)).getElementsByTagName("url");
			if(url == null || url.getLength() == 0) {
				System.out.println("no scm/url tag found");
				return null;
			}
			return url.item(0).getFirstChild().getNodeValue();

		} catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
		}

		return getPomUrl(jarName, version).getPath();
	}

	private static URL getPomUrl(String jarName, String version) {
		try {
			URL url = new URL("https://dais-maven.mpi-cbg.de/service/rest/v1/search/assets?name=" + jarName + "&maven.extension=pom&maven.classifier");
			JSONObject pomObj = new JSONObject(fromURL(url));
			JSONArray items = pomObj.getJSONArray("items");
			if(items == null || items.length() == 0) return null;
			JSONObject entry = (JSONObject) items.get(0);
			String downloadUrl = entry.getString("downloadUrl");
			return new URL(downloadUrl);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	static String fromURL(URL url) throws IOException {
//		System.out.println("Loading " + url);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		// !!!!! use the following line to authorize and get not bugged by API call limitations:
//		conn.addRequestProperty("Authorization", "token YOUR_OAUTH_TOKEN_HERE");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

		String line;
		String output = "";
		while ((line = br.readLine()) != null) {
			output += line;
		}
		if(output.isEmpty()) return null;
		conn.disconnect();
		return output;
	}

	public static void main( final String... args ) throws ParserConfigurationException, SAXException, IOException {
		System.out.println(fromURL(new URL("https://api.github.com")));
		FilesCollection files = new FilesCollection(new File("/home/random/Programs/Fiji.app"));
		System.out.println(files.prefix(""));
		files.read();
		Set<String> urls = new HashSet<>();
		files.forEach(file -> {
			String name = file.getFilename(true).replace("jars/", "").replace(".jar", "");
			String version = file.getFilename(false).replace("jars/", "").replace(".jar", "").replace(name, "");
			if(version.startsWith("-")) version = version.substring(1);
			if(!version.isEmpty()) {
//				System.out.println("getting github URL for " + name + ", " + version);
				String url = getGithubUrl(name, version);
				if (url == null) return;
				if (!url.startsWith("https://github.com")) return;
				urls.add(url);
				System.out.println(url);
			}
		});

		System.out.println("loading contributors");
		Map<String, Integer> contributors = new HashMap<>();
		urls.forEach(url -> {
			try {
				getContributors(url).forEach((login, contributions) -> {
					if (contributors.containsKey(login)) {
						contributors.put(login, contributors.get(login) + contributions);
					} else {
						contributors.put(login, contributions);
					}
				});
				System.out.println(url);
			}
			catch (JSONException | IOException e) {
				e.printStackTrace();
				ValueComparator bvc = new ValueComparator(contributors);
				TreeMap<String, Integer> sorted_map = new TreeMap<>(bvc);
				sorted_map.putAll(contributors);
				sorted_map.forEach((login, contributions) -> {
					System.out.print(login + " (" + contributions + "), ");
				});
				System.out.println();
			}
		});
		ValueComparator bvc = new ValueComparator(contributors);
		TreeMap<String, Integer> sorted_map = new TreeMap<>(bvc);
		sorted_map.putAll(contributors);
		sorted_map.forEach((login, contributions) -> {
			System.out.print(login + " (" + contributions + "), ");
		});
		System.out.println();
	}

	static class ValueComparator implements Comparator<String> {
		Map<String, Integer> base;

		public ValueComparator(Map<String, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(String a, String b) {
			if (base.get(a) >= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}

	private static Map<String, Integer> getContributors(String url) throws JSONException, IOException {
		String repo = url.replace("https://github.com", "");
		Map<String, Integer> contributors = new HashMap<>();
		JSONArray contributorsObj = null;
		try {
			contributorsObj = new JSONArray(fromURL(new URL(githubApi + "/repos" + repo + "/contributors")));
		}
		catch(RuntimeException e) {
			e.printStackTrace();
			return contributors;
		}
		for (int i = 0; i < contributorsObj.length(); i++) {
			String login = (String) ((JSONObject) contributorsObj.get(i)).get("login");
			int contributions = (int) ((JSONObject) contributorsObj.get(i)).get("contributions");
			contributors.put(login, contributions);
		}
		return contributors;
	}
}
