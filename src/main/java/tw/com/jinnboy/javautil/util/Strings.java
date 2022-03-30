package tw.com.jinnboy.javautil.util;

import java.util.ArrayList;

/*
 * @author 阿昌
 *
 */
public class Strings {

	private Strings() {
	}

	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	public static boolean notEmpty(String str) {
		return str != null && str.length() != 0;
	}

	public static String requireNonEmpty(String str, String message) {
		if (str == null || str.length() == 0) {
			throw new NullPointerException(message);
		}
		return str;
	}

	public static String arrayToString(String[] array) {
		if (array == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(32);
		sb.append('[');
		boolean isFirst = true;
		for (String s : array) {
			if (isFirst) {
				isFirst = false;
			} else {
				sb.append(", ");
			}
			sb.append(s);
		}
		sb.append(']');
		return sb.toString();
	}

	// 回傳第一個非null的字串
	public static String first(Object... values) {
		String str = null;
		for (Object value : values) {
			if (value == null) {
				continue;
			}
			str = value.toString();
			if (str.length() == 0) {
				continue;
			}
			return str;
		}
		return str;
	}

	// 回傳str本身，如果str為null則回傳空字串。(確保null顯示空字串，而不是"null")
	public static String toString(String str) {
		return str == null ? "" : str;
	}

	// 回傳obj.toString()，如果obj是null則回傳空字串。
	public static String toString(Object obj) {
		return obj == null ? "" : obj.toString();
	}

	public static String fixedWidthNumber(Number num, int width) {
		return fixedWidthDecimal(num, width, 0);
	}

	public static String fixedWidthDecimal(Number num, int width, int scale) {
		if (num == null) {
			num = 0;
		}
		String str = String.format("%." + scale + "f", num.doubleValue());
		str = str.charAt(0) == '-' ? '-' + leftPad(str.substring(1), width, '0', false)
				: leftPad(str, width, '0', false);
		if (str.length() > width) { // 數字太大超過長度，直接返回問號，讓系統吃檔錯誤，比較容易被發現。
			return leftPad("", width, '?', false);
		}
		return str;
	}

	private static boolean isDigitChar(char ch) {
		return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
	}

	public static String[] split(String str, String delimiter) {
		return split(str, delimiter, 0, false, false);
	}

	// JAVA雖有內建String.split()，但此函式是加強版。
	// 特點1: 若analyzeEscapeChar=true，雙引號內的文字不會被切割，且會自動轉換跳脫字元。
	// 特點2: 當分析字串str="|a|b||c"時，str.split()的結果為[a,b,c]，首尾和重複的delimiter會被省略。
	// Strings.split(str)依照ignoreEmpty的false或true，結果為[,a,b,,c]或[a,b,c]。
	public static String[] split(String str, String delimiter, int limit, boolean analyzeEscapeChar,
			boolean ignoreEmpty) {
		if (isEmpty(str) || isEmpty(delimiter) || (limit == 1 && !analyzeEscapeChar)) {
			if (ignoreEmpty && isEmpty(str)) {
				return new String[0];
			} else {
				return new String[] { str };
			}
		}
		ArrayList<String> list = new ArrayList<>();
		boolean isAfterSlash = false; // 前一個字是斜線，要比對跳脫字元
		boolean isInQuotationMark = false; // 在雙引號裡，就算遇到delimiter也不需要切割
		String escapeChars = "tbnrf'\"\\";
		String escapeChars2 = "\t\b\n\r\f'\"\\";
		int limitTimes = limit == 0 ? Integer.MAX_VALUE : limit - 1;
		int i = 0;
		int length = str.length();
		char delimiterFirstChar = delimiter.charAt(0);
		int delimiterLength = delimiter.length();
		char ch;
		StringBuilder sb = new StringBuilder();
		String s;
		while (i < length) {
			ch = str.charAt(i);
			if (isAfterSlash) {
				int j = escapeChars.indexOf(ch);
				if (j != -1) {
					sb.append(escapeChars2.charAt(j));
					i++;
				} else if (ch == 'u' && i + 4 < length && isDigitChar(str.charAt(i + 1))
						&& isDigitChar(str.charAt(i + 2)) && isDigitChar(str.charAt(i + 3))
						&& isDigitChar(str.charAt(i + 4))) {
					ch = (char) Integer.parseInt(str.substring(i + 1, i + 5), 16);
					sb.append(ch);
					i += 5;
				} else {
					sb.append('\\');
				}
				isAfterSlash = false;
			} else if (ch == '"') {
				if (isInQuotationMark) {
					isInQuotationMark = false;
					i++;
				} else if (analyzeEscapeChar) {
					isInQuotationMark = true;
					i++;
				} else {
					sb.append(ch);
					i++;
				}
			} else if (analyzeEscapeChar && ch == '\\') {
				isAfterSlash = true;
				i++;
			} else if (list.size() < limitTimes && !isInQuotationMark && ch == delimiterFirstChar
					&& str.regionMatches(i, delimiter, 0, delimiterLength)) {
				s = sb.toString();
				sb.setLength(0);
				if (!ignoreEmpty || notEmpty(s)) {
					list.add(s);
				}
				i += delimiterLength;
			} else {
				sb.append(ch);
				i++;
			}
		}
		s = sb.toString();
		sb.setLength(0);
		if (!ignoreEmpty || notEmpty(s)) {
			list.add(s);
		}
		return list.toArray(new String[list.size()]);
	}

	public static String center(String str, int width) {
		return pad(str, width, ' ', false, 0);
	}

	public static String leftPad(String str, int width, char padChar, boolean trimWhenOverflow) {
		return pad(str, width, padChar, false, 1);
	}

	public static String rightPad(String str, int width, char padChar, boolean trimWhenOverflow) {
		return pad(str, width, padChar, false, 2);
	}

	// direction: 0=both sides; 1=left; 2=right
	private static String pad(String str, int width, char padChar, boolean trimWhenOverflow, int direction) {
		if (str == null) {
			str = "";
		}
		int length = str.length();
		if (length == width) {
			return str;
		} else if (length > width) {
			return trimWhenOverflow ? str.substring(0, width) : str;
		}
		StringBuilder sb = new StringBuilder(width);
		if (direction == 0) {
			for (int i = (width - length) / 2; i > 0; i--) {
				sb.append(padChar);
			}
		} else if (direction == 1) {
			for (int i = (width - length); i > 0; i--) {
				sb.append(padChar);
			}
		}
		sb.append(str);
		for (int i = sb.length(); i < width; i++) {
			sb.append(padChar);
		}
		return sb.toString();
	}

}
