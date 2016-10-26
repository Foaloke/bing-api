package it.unipi.di.acube.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Downloader {

	private static final File UNREACHABLE_URLS_LOG = new File("scraped/unreachable_urls.log");
	private static final String USER_AGENT = "matteo.tonnicchi@gmail.com";
	public static final String SCRAPED_DIR_PATH = "scraped";
	public static final String CLEANED_DIR_PATH = "cleaned";
	public static final String REGEXES_DIR_PATH = "regexes";

	public static String download(String address) {
		return download(USER_AGENT, address);
	}

	public static String download(String userAgent, String address) {

		if (isKnownUnreachable(address)) {
			return "";
		}

		System.setProperty("http.agent", userAgent);

		try(InputStream is = toURL(address).openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
			return br.lines().collect(Collectors.joining("\n"));
		} catch (FileNotFoundException fnfe) {
			updateUnreachableUrls(address);
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return "";

	}

	private static URL toURL(String address) {
		try {
			return new URL(address);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Can not create a URL from "+address);
		}
	}

	private static void updateUnreachableUrls(String address) {
		try {
			if (!isKnownUnreachable(address)) {
				Files.append(address + "\n", UNREACHABLE_URLS_LOG, Charsets.UTF_8);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not update unreachable urls log file", e);
		}
	}

	public static boolean isKnownUnreachable(String address) {
		try {
			return Files.readLines(UNREACHABLE_URLS_LOG, Charsets.UTF_8).contains(address);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read unreachable urls log file", e);
		}
	}

	private static String composeDestinationPathFor(String root, String sourceKey, String address, String extension) {
		try {
			return composeSourceFolder(root, encode(sourceKey)) + File.separator + encodeAsFileName(address, extension);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the destination path ", e);
		}
	}

	private static String composeSourceFolder(String root, String sourceKey) {
		try {
			return root + File.separator + URLEncoder.encode(sourceKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the source folder path ", e);
		}
	}

	public static void writeFile(String root, String sourceKey, String address, String content) {
		writeFile(root, sourceKey, address, content, "html");
	}
	
	public static void writeFile(String root, String sourceKey, String address, String content, String extension) {
		String path = composeDestinationPathFor(root, sourceKey, address, extension);
		try {
			File file = new File(path);
			Files.createParentDirs(file);
			Files.write(content, file, Charsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Error while writing file to " + path, e);
		}
	}

	public static void appendToFile(String root, String sourceKey, String address, String content, String extension) {
		String path = composeDestinationPathFor(root, sourceKey, address, extension);
		try {
			File file = new File(path);
			Files.createParentDirs(file);
			Files.append(content, file, Charsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Error while writing file to " + path, e);
		}
	}

	private static String encode(String folderName) {
		try {
			return URLEncoder.encode(folderName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Can not encode the folder name " + folderName, e);
		}
	}

	private static String encodeAsFileName(String address, String extension) throws UnsupportedEncodingException {
		String encoded
			= URLEncoder.encode(address.replaceFirst("http://", ""), "UTF-8")
				.replaceAll("%2F", File.separator);
		int lastSeparatorIndex = encoded.lastIndexOf(File.separator);
		encoded = lastSeparatorIndex == -1 ? encoded : encoded.substring(lastSeparatorIndex+1);
		return encoded + "." + extension;
	}

}
