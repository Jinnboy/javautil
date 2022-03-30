package tw.com.jinnboy.javautil.util;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * 使用方式：
 * 1.建立和jar檔同名的cfg檔案，放到jar檔相同的目錄。
 * 2.Config.setCategory();
 * 3.直接呼叫Config.get(param)取得相關設定的值。
 * 
 * Config會在第一次get時，找尋和jar檔或執行目錄裡同名的cfg檔案，並自動載入檔案全部內容。
 * Config.get取值順序：先看有沒有對應Category的section，再找global空白section。
 * 
 * cfg檔案格式範例如下。
 * ====================
 * GlobalParam1=XXX
 * GlobalParam2=YYY
 * 
 * # Use number sign (#) to indicate that this line of text is a comment
 * 
 * [CategoryA]
 * param1=aaa
 * param2=apple
 * This is an apple blabla...
 * 
 * [CategoryB]
 * param1=bbb
 * param2=banana
 * ====================
 * 
 * 設定Config.setCategory("CategoryA")後，呼叫Config.get("param1")會得到aaa。
 * 設定Config.setCategory("CategoryB")後，呼叫Config.get("param1")會得到bbb。
 * 
 * 不是等號格式(param=value)的文字會統一存成list，
 * 例如在設定CategoryA後，呼叫Config.getOtherLines()可以取得有字串「This is apple blabla...」的list。
 * 
 * @author 阿昌
 *
 */
public class Config {
	private static Hashtable<String, Object> table;
	private static Hashtable<String, Boolean> categories;
	private static String category;
	private static String environment; // environment

	private Config() {
	}

	public static String getCategory() {
		return Config.category;
	}

	public static void setCategory(String category) {
		Config.category = category;
	}

	public static boolean containCategory(String category) {
		if (categories == null) {
			load();
		}
		return categories != null && categories.get(category.toLowerCase()) != null;
	}

	private static String checkEnvironment() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			if (hostname == null || hostname.length() <= 3) {
				return "LOCAL";
			}
			char ch0 = Character.toUpperCase(hostname.charAt(0));
			char ch1 = Character.toUpperCase(hostname.charAt(1));
			if (ch0 == 'P' && ch1 != 'C') {
				return "PROD";
			} else if (ch0 == 'T') {
				return "UAT";
			} else if (ch0 == 'D') {
				return "DEV";
			} else {
				return "LOCAL";
			}
		} catch (UnknownHostException e) {
			// e.printStackTrace();
			return "LOCAL";
		}
	}

	public static String getEnvironment() {
		if (environment == null) {
			environment = checkEnvironment();
		}
		return environment;
	}

	public static void setEnvironment(String environment) {
		Config.environment = environment;
	}

	// 直接用呼叫它的class名稱來當作section title
	public static String get(String param) {
		return getValue(category, param, "");
	}

	// getOrDefault
	public static String get(String param, String defaultValue) {
		return getValue(category, param, defaultValue);
	}

	// getOrThrow
	public static String getOrThrow(String param) throws Exception {
		String value = getValue(category, param, null);
		if (value != null) {
			return value;
		}
		throw new Exception(String.format("Can not found '%s' in config.", param));
	}

	public static String getValue(String category, String param, String defaultValue) {
		if (table == null) {
			load();
		}
		Object value;
		// 先檢查有沒有category的param
		if (category != null && category.length() > 0) {
			value = table.get(category.toLowerCase() + '@' + param);
			if (value != null) {
				return value.toString();
			}
		}
		value = table.get(param);// 最後才檢查有沒有global的param
		return (null == value) ? defaultValue : value.toString();
	}

	public static List<String> getOtherLines() {
		return getOtherLines(category);
	}

	@SuppressWarnings("unchecked")
	public static List<String> getOtherLines(String category) {
		if (category != null && category.length() > 0) {
			category = category.toLowerCase() + '@';
		}
		Object value = table.get(category);
		return value == null ? Collections.emptyList() : (List<String>) value;
	}

	public static boolean load() {
		Path cfgPath = null;
		try {
			// classPath in jar=aaa/bbb/Config.class
			// classPath in eclipse=/D:/eclipse-workspace/project/bin/aaa/bbb/Config.class
			String classPath = Config.class.getResource(Config.class.getSimpleName() + ".class").getFile();
			if (classPath != null && !classPath.startsWith("/")) {
				// jarPath=jar:file:/appool/batch/project/project.jar!/aaa/bbb/Config.class
				// jarPath=jar:file:/D:/project/project.jar!/aaa/bbb/Config.class
				String jarPath = ClassLoader.getSystemClassLoader().getResource(classPath).toString();
				if (jarPath.startsWith("jar:file:")) {
					String str = jarPath.substring(9, jarPath.toLowerCase().lastIndexOf(".jar!")) + ".cfg";
					if (str.indexOf(':') > 0 && str.charAt(0) == '/') {// 如果有:字元，可能是Windows路徑/D:，要把開頭的/去掉
						str = str.substring(1);
					}
					Path path = Paths.get(str);
					if (Files.exists(path)) {
						cfgPath = path;
						// System.out.println(String.format("Find the config file(%s) in jar path.",
						// path.getFileName()));
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Find jar file has error:" + e);
		}
		if (cfgPath == null) {
			try {
				Path path = Paths.get(Paths.get("").toAbsolutePath().getFileName().toString() + ".cfg");
				if (Files.exists(path)) {
					cfgPath = path;
					// System.out.println(String.format("Find the config file(%s) in execution
					// path", path.getFileName()));
				}
			} catch (Exception e) {
				System.out.println("Find cfg file has error:" + e);
			}
		}
		if (cfgPath != null) {
			return load(cfgPath);
		}
		if (table == null) {
			table = new Hashtable<>();
		} else {
			table.clear();
		}
		return false;
	}

	public static boolean load(String filename) {
		Path path = Paths.get(filename);
		if (Files.exists(path)) {
			return load(path);
		} else {
			System.out.println(String.format("Config.load(%s) Not found config file.", filename));
			if (table == null) {
				table = new Hashtable<>();
			} else {
				table.clear();
			}
			return false;
		}
	}

	public static boolean isValidParam(String param) {
		char ch;
		for (int i = 0, len = param.length(); i < len; i++) {
			ch = param.charAt(i);
			if (ch != '_' && (ch < '0' || ch > '9') && (ch < 'A' || ch > 'Z') && (ch < 'a' || ch > 'z')) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static boolean load(Path path) {
		// System.out.println(String.format("Start loading config: %s", path));
		if (table == null) {
			table = new Hashtable<>();
		} else {
			table.clear();
		}
		if (categories == null) {
			categories = new Hashtable<>();
		} else {
			categories.clear();
		}
		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String section = "";
			String line;
			String param;
			int i;
			List<String> list;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				if (line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']') {
					section = line.substring(1, line.length() - 1).toLowerCase();
					categories.put(section, true);
					section += '@';
					continue;
				}
				i = line.indexOf('=');
				if (i > 0 && isValidParam(param = line.substring(0, i).trim())) {
					table.put(section + param, line.substring(i + 1).trim());
					continue;
				}
				list = (List<String>) table.get(section);
				if (list == null) {
					list = new ArrayList<>(4);
					table.put(section, list);
				}
				list.add(line);
			}
			// System.out.println("Complete loading config.");
			return true;
		} catch (Exception e) {
			System.out.println(String.format("Config.load(%s) has error: %s", path, e));
			// e.printStackTrace();
		}
		return false;
	}
}
