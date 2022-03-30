package tw.com.jinnboy.javautil.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Paths {

	private static String jarFolder;

	private Paths() {
	}

	public static Path get(String filename) {
		return FileSystems.getDefault().getPath(filename);
	}

	public static Path get(String folder, String filename) {
		return FileSystems.getDefault().getPath(concat(folder, filename));
	}

	/**
	 * 如果filename是絕對路徑，則直接回傳Paths.get(filename);
	 * 如果filename是相對路徑，則把folder當作父目錄，回傳folder底下的filename檔案。
	 */
	public static Path get(Path folder, String filename) {
		return isAbsolutePath(filename) ? Paths.get(filename) : folder.resolve(filename);
	}

	/**
	 * 回傳filename是否為絕對路徑。
	 */
	public static boolean isAbsolutePath(String filename) {
		return filename != null && filename.length() > 1 && (filename.charAt(0) == '/' || filename.charAt(0) == '\\'
				|| filename.charAt(0) == '~' || filename.indexOf(':') != -1);
	}

	/**
	 * 如果filename是絕對路徑，則直接回傳filename; 如果filename是相對路徑，則回傳folder+filename。
	 */
	public static String concat(String folder, String filename) {
		return isAbsolutePath(filename) ? filename
				: Strings.isEmpty(folder) ? filename : endWithSlash(folder) + filename;
	}

	/**
	 * 回傳jar所在的資料夾路徑。
	 */
	public static String jarFolder() {
		if (jarFolder == null) {
			try {
				// classPath in jar=aaa/bbb/Paths.class
				// classPath in eclipse=/D:/eclipse-workspace/project/bin/aaa/bbb/Paths.class
				String classPath = Config.class.getResource(Paths.class.getSimpleName() + ".class").getFile();
				if (classPath != null && !classPath.startsWith("/")) {
					// jarPath=jar:file:/appool/batch/project/project.jar!/aaa/bbb/Paths.class
					// jarPath=jar:file:/D:/project/project.jar!/aaa/bbb/Paths.class
					String jarPath = ClassLoader.getSystemClassLoader().getResource(classPath).toString();
					if (jarPath.startsWith("jar:file:")) {
						String str = jarPath.substring(9,
								jarPath.lastIndexOf('/', jarPath.toLowerCase().lastIndexOf(".jar!")) + 1);
						if (str.indexOf(':') > 0 && str.charAt(0) == '/') {// 如果有:字元，可能是Windows路徑/D:，要把開頭的/去掉
							str = str.substring(1);
						}
						if (Files.exists(Paths.get(str))) {
							jarFolder = str;
						}
					}
				} else {
					jarFolder = "";
				}
			} catch (Exception e) {
				jarFolder = "";
			}
		}
		return jarFolder;
	}

	/**
	 * 建立檔案path所需的目錄，並檢查如果有既有檔案，就把既有檔案改名。
	 */
	public static Path preparePath(Path path) throws IOException {
		Path folder = path.getParent();
		if (folder != null) {
			Files.createDirectories(path.getParent());
		}
		Paths.renameOldFile(path);
		return path;
	}

	public static void renameOldFile(Path path) {
		try {
			if (Files.exists(path)) {
				Path bakPath = nonduplicatedPath(path, "bak");
				Files.move(path, bakPath);
			}
		} catch (Exception e) {
			// e.printStackTrace();
			Log.error(e, "renameOldFile(%s) failed.", path);
		}
	}

	// 若path已經有檔案存在，則將既有檔案重新命名。
	public static Path nonduplicatedPath(Path path) {
		return nonduplicatedPath(path, "");
	}

	/**
	 * 若path已經有檔案存在，則將既有檔案重新命名。 例如path的檔名為F.txt，檢查有同名檔案時，
	 * 若prefix為空白，命名規則為：F(2).txt、F(3).txt、F(4).txt。
	 * 若prefix為bak，命名規則為：F(bak1).txt、F(bak2).txt、F(bak3).txt。
	 * 
	 * @param path
	 * @param prefix 定義命名規則。
	 * @return path 回傳輸入的path。
	 */
	public static Path nonduplicatedPath(Path path, String prefix) {
		if (!Files.exists(path)) {
			return path;
		}
		String name = path.getFileName().toString();
		String extension = "";
		int i = name.lastIndexOf('.');
		if (i > 0 && i >= name.length() - 7) { // 假設附檔名最多6個字
			extension = name.substring(i);
			name = name.substring(0, i);
		}
		Path parent = path.toAbsolutePath().getParent();
		Path newPath = null;
		if (Strings.notEmpty(prefix)) {
			for (int n = 1; n < 1000; n++) {
				newPath = parent.resolve(name + '(' + prefix + n + ')' + extension);
				if (!Files.exists(newPath)) {
					return newPath;
				}
			}
		} else {
			for (int n = 2; n < 1000; n++) {
				newPath = parent.resolve(name + '(' + n + ')' + extension);
				if (!Files.exists(newPath)) {
					return newPath;
				}
			}
		}
		return path;
	}

	public static void moveToArchiveIfExists(Path path) {
		if (path != null && Files.exists(path)) {
			moveToFolder(path, "_archive/" + Dates.replaceSymbol("yyyyMMdd"));
		}
	}

	public static void moveToFailedIfExists(Path path) {
		if (path != null && Files.exists(path)) {
			moveToFolder(path, "_failed/" + Dates.replaceSymbol("yyyyMMdd"));
		}
	}

	public static void moveToFolder(Path path, String folderName) {
		try {
			Path folder = path.resolveSibling(folderName);
			if (!Files.exists(folder)) {
				Files.createDirectories(folder);
			}
			Path newPath = folder.resolve(path.getFileName().toString());
			renameOldFile(newPath);
			// newPath = nonduplicatedPath(newPath);
			Files.move(path.toAbsolutePath(), newPath);
		} catch (Exception e) {
			// e.printStackTrace();
			Log.error(e, "moveToFolder(%s, %s) failed.", path, folderName);
		}
	}

	/**
	 * 去掉路徑，返回path的名稱
	 * 
	 * @param path
	 * @return path的檔案名稱(包含附檔名)
	 */
	public static String getName(String path) {
		int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		path = path.substring(i + 1);
		return path;
	}

	/**
	 * 去掉路徑和副檔名，返回path的名稱，且不包含副檔名。
	 * 
	 * @param path
	 * @return path的檔案名稱，不包含附檔名。
	 */
	public static String getSimpleName(String path) {
		return getSimpleName(path, false);
	}

	/**
	 * 去掉路徑和副檔名，返回path的名稱，且不包含副檔名。 若trimDate=true，檔名如果後面有日期數字yyyymmdd也會一起移除。
	 * 
	 * @param path
	 * @param trimDate 移除檔名後面的日期
	 * @return path的檔案名稱，不包含附檔名。
	 */
	public static String getSimpleName(String path, boolean trimDate) {
		int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		String name = path.substring(i + 1);
		i = name.lastIndexOf('.');
		if (i > 0) {
			name = name.substring(0, i);
		}
		int length = name.length();
		if (trimDate && length > 8) {
			String suffix = name.substring(length - 8);
			if (Regex.matches(suffix, "yyyymmdd|\\d{8}", Pattern.CASE_INSENSITIVE)) {
				name = name.substring(0, length - 8);
			}
		}
		return name;
	}

	/**
	 * 獲得filename的副檔名，不包含.點。
	 */
	public static String getExtension(String filename) {
		String extension = Regex.find(filename, "\\.\\w{1,6}$");
		return extension != null ? extension.substring(1) : extension;
	}

	/**
	 * 確保folder最後一個字元是斜線。 主要用於folder和filename的字串相加。
	 */
	public static String endWithSlash(String folder) {
		if (Strings.isEmpty(folder)) {
			return folder;
		}
		char ch = folder.charAt(folder.length() - 1);
		if (ch != '/' && ch != '\\') {
			return folder.indexOf('\\') > 0 ? folder + '\\' : folder + '/';
		}
		return folder;
	}

}
