package tw.com.jinnboy.javautil.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * 簡單方便的Log類別，只要定義好LogPath，之後直接呼叫Log.info()即可，不需要其他設定。
 * 每次呼叫info()、warning()、error()時，都會getStackTrace()一下以紀錄呼叫的類別和函式名稱，簡單暴力樸實無華。
 * 
 * 考慮效能問題，可以設定InfoWriteClassMethod=false讓info()不必找尋函式名稱。
 * 
 * 若需要同時寫第二個Log，可以直接建立新的Logger。
 * log.Logger logger = new log.Logger("xxx.log");
 * 
 * @author 阿昌
 */
public class Log {
	private static final SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static String logPath = "log/javautil-yyyy-MM-dd.log";
	private static Logger logger;
	private static boolean InfoWriteClassMethod = "1".equals(Config.get("InfoWriteClassMethod", "1"));

	static {
		String logPathStr = Args.get("logPath");
		String category = Args.getCategory();
		if (Strings.notEmpty(logPathStr)) {
			logPath = logPathStr;
		} else if (Strings.notEmpty(category)) {
			logPath = "log/javautil-" + category + "-yyyy-MM-dd.log";
		} else {
			logPath = "log/javautil-yyyy-MM-dd.log";
		}
		logger = new Logger(logPath);
	}

	private Log() {
	}

	public static void setLogPath(String logPath) {
		logger.setLogPath(logPath);
	}

	private static String classSimpleName(String name) {
		int i = name.lastIndexOf('.');
		return i != -1 ? name.substring(i + 1) : name;
	}

	private static String getCallingClassMethod() {
		// 等升級到Java 9可改成使用StackWalker
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		// elements[0]=Thread.getStackTrace()
		// elements[1]=Log.callingClassMethod()
		// elements[2]=Log.info()
		// elements[3]=the calling class
		if (elements.length >= 3) {
			return classSimpleName(elements[3].getClassName()) + '-' + elements[3].getMethodName();
		}
		return null;
	}

	private static String errorMsg(String message, Throwable thrown) {
		if (thrown != null) {
			StackTraceElement[] stackTraceElements = thrown.getStackTrace();
			StringBuilder sb = new StringBuilder(128);
			sb.append(message);
			sb.append("\r\n");
			sb.append(thrown);
			sb.append("\r\n");
			for (StackTraceElement stackTraceElement : stackTraceElements) {
				sb.append("\tat ");
				sb.append(stackTraceElement);
				sb.append("\r\n");
			}
			return sb.toString();
		}
		return message;
	}

	private static String format(Date time, char level, String classMethod, String message) {
		StringBuilder sb = new StringBuilder(128);
		sb.append('[');
		sb.append(sfd.format(time));
		sb.append(']');
		sb.append(' ');
		sb.append(level);
		if (classMethod != null && classMethod.length() > 0) {
			sb.append(' ');
			sb.append(classMethod);
		}
		sb.append(':');
		sb.append(' ');
		sb.append(message);
		sb.append("\r\n");
		return sb.toString();
	}

	public static void info(String format, Object... args) {
		String message = String.format(format, args);
		logger.write(new Date(), 'I', InfoWriteClassMethod ? getCallingClassMethod() : null, message);
	}

	public static void info(String message) {
		logger.write(new Date(), 'I', InfoWriteClassMethod ? getCallingClassMethod() : null, message);
	}

	public static void warning(String format, Object... args) {
		String message = String.format(format, args);
		logger.write(new Date(), 'W', getCallingClassMethod(), message);
	}

	public static void warning(String message) {
		logger.write(new Date(), 'W', getCallingClassMethod(), message);
	}

	public static void error(String format, Object... args) {
		String message = String.format(format, args);
		logger.write(new Date(), 'E', getCallingClassMethod(), message);
	}

	public static void error(Throwable thrown, String format, Object... args) {
		String message = String.format(format, args);
		logger.write(new Date(), 'E', getCallingClassMethod(), errorMsg(message, thrown));
	}

	public static class Logger {
		private String logPath;
		private BufferedWriter bw;
		private long expiration = 0;

		public Logger(String logPath) {
			this.logPath = logPath;
		}

		public void setLogPath(String logPath) {
			this.logPath = logPath;
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
				bw = null;
			}
		}

		public synchronized void close() {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
				bw = null;
			}
		}

		public synchronized void write(Date time, char level, String classMethod, String message) {
			try {
				if (time.getTime() > expiration) {
					close();
				}
				String str = Log.format(time, level, classMethod, message);
				if (level == 'E') {
					System.err.print(str);
				}
				if (bw == null) {
					Path path = Paths.get(Paths.jarFolder(), Dates.replaceSymbol(logPath));
					Files.createDirectories(path.toAbsolutePath().getParent());
					bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
					long offset = TimeZone.getDefault().getRawOffset();
					long now = time.getTime();
					long dayMillis = TimeUnit.DAYS.toMillis(1);
					expiration = now + dayMillis - ((now + offset) % dayMillis);
				}
				bw.write(str);
				bw.flush();
			} catch (IOException e) {
				close();
			}
		}

		private String getCallingClassMethod() {
			// 等升級到Java 9可改成使用StackWalker
			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
			// elements[0]=Thread.getStackTrace()
			// elements[1]=Log.callingClassMethod()
			// elements[2]=LogInstance.info()
			// elements[3]=the calling class
			if (elements.length >= 3) {
				return classSimpleName(elements[3].getClassName()) + '-' + elements[3].getMethodName();
			}
			return null;
		}

		public void info(String format, Object... args) {
			String message = String.format(format, args);
			write(new Date(), 'I', InfoWriteClassMethod ? Log.getCallingClassMethod() : null, message);
		}

		public void info(String message) {
			write(new Date(), 'I', InfoWriteClassMethod ? Log.getCallingClassMethod() : null, message);
		}

		public void warning(String format, Object... args) {
			String message = String.format(format, args);
			write(new Date(), 'W', getCallingClassMethod(), message);
		}

		public void warning(String message) {
			write(new Date(), 'W', getCallingClassMethod(), message);
		}

		public void error(String format, Object... args) {
			String message = String.format(format, args);
			write(new Date(), 'E', getCallingClassMethod(), message);
		}

		public void error(Throwable thrown, String format, Object... args) {
			String message = String.format(format, args);
			write(new Date(), 'E', getCallingClassMethod(), errorMsg(message, thrown));
		}
	}

}
