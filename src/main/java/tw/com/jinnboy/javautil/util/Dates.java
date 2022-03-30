package tw.com.jinnboy.javautil.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dates {
	private static HashMap<String, SimpleDateFormat> dateFormats = new HashMap<>();

	private Dates() {
	}

	public static SimpleDateFormat getSimpleDateFormat(String fmt) {
		SimpleDateFormat sdf = dateFormats.get(fmt);
		if (sdf == null) {
			sdf = new SimpleDateFormat(fmt);
			dateFormats.put(fmt, sdf);
		}
		return sdf;
	}

	public static String format(String fmt, Date date) {
		return getSimpleDateFormat(fmt).format(date);
	}

	public static Date parse(String str, String fmt) {
		try {
			return getSimpleDateFormat(fmt).parse(str);
		} catch (ParseException e) {
			throw new DateTimeException(e.getMessage(), e.getCause());
		}
	}

	public static Date parseOrNull(String str, String fmt) {
		try {
			return getSimpleDateFormat(fmt).parse(str);
		} catch (ParseException e) {
			return null;
		}
	}

	public static String DateString() {
		return DateString(new Date());
	}

	public static String DateString(Date date) {
		return date == null ? "" : format("yyyy-MM-dd", date);
	}

	public static String TimeString() {
		return TimeString(new Date());
	}

	public static String TimeString(Date date) {
		return date == null ? "" : format("yyyy-MM-dd HH:mm:ss", date);
	}

	public static Date today() {
		long now = System.currentTimeMillis();
		return new Date(now - ((now + TimeZone.getDefault().getRawOffset()) % 86400000));
	}

	public static String replaceSymbol(String str) {
		return replaceSymbol(str, new Date());
	}

	// 把str裡的yyyy、MM、dd、HH、mm、ss、SSS取代成年、月、日、時、分、秒、毫秒。
	// 並把$today、$yesterday、$tomorrow轉成yyyymmdd，$lastmonth、$nextmonth轉成yyyymm。
	// 很適合用來把檔名或設定變數裡的日期符號轉成對應的數字。
	public static String replaceSymbol(String str, Date date) {
		if (str == null) {
			return str;
		}
		StringBuilder sb = new StringBuilder(str);
		String yyyymmdd = format("yyyyMMdd", date);
		String time = format("HHmmssSSS", date);
		// String yesterday = format("yyyyMMdd", addDay(date, -1));
		// 比對條件一定要有yy和mm，dd可以不用有。
		Matcher matcher = Regex.pattern("(yy)?(yy)[^a-zA-Z0-9]?(mm)[^a-zA-Z0-9]?(dd)?", Pattern.CASE_INSENSITIVE)
				.matcher(str);
		int index;
		while (matcher.find()) {
			for (int groupIndex = 1; groupIndex <= 4; groupIndex++) {
				if (matcher.group(groupIndex) != null) {
					index = matcher.start(groupIndex);
					sb.setCharAt(index, yyyymmdd.charAt(groupIndex * 2 - 2));
					sb.setCharAt(index + 1, yyyymmdd.charAt(groupIndex * 2 - 1));
				}
			}
		}
		// 沒有yy，只有mm和dd的情況
		matcher = Regex.pattern("(mm)[^a-zA-Z0-9]?(dd)", Pattern.CASE_INSENSITIVE).matcher(sb.toString());
		while (matcher.find()) {
			index = matcher.start(1);
			sb.setCharAt(index, yyyymmdd.charAt(4));
			sb.setCharAt(index + 1, yyyymmdd.charAt(5));
			index = matcher.start(2);
			sb.setCharAt(index, yyyymmdd.charAt(6));
			sb.setCharAt(index + 1, yyyymmdd.charAt(7));
		}
		matcher = Regex.pattern("(hh):?(mm):?(ss)?(?:\\.?(SSS))?", Pattern.CASE_INSENSITIVE).matcher(sb.toString());
		while (matcher.find()) {
			for (int groupIndex = 1; groupIndex <= 4; groupIndex++) {
				if (matcher.group(groupIndex) != null) {
					index = matcher.start(groupIndex);
					sb.setCharAt(index, time.charAt(groupIndex * 2 - 2));
					sb.setCharAt(index + 1, time.charAt(groupIndex * 2 - 1));
					if (groupIndex == 4) {
						sb.setCharAt(index + 2, time.charAt(8));
					}
				}
			}
		}
		str = sb.toString();
		if (str.contains("$today")) {
			str = str.replace("$today", yyyymmdd);
		}
		if (str.contains("$yesterday")) {
			str = str.replace("$yesterday", format("yyyyMMdd", addDay(date, -1)));
		}
		if (str.contains("$tomorrow")) {
			str = str.replace("$tomorrow", format("yyyyMMdd", addDay(date, 1)));
		}
		if (str.contains("$lastmonth")) {
			str = str.replace("$lastmonth", format("yyyyMM", addMonth(date, -1)));
		}
		if (str.contains("$nextmonth")) {
			str = str.replace("$nextmonth", format("yyyyMM", addMonth(date, 1)));
		}
		return str;
	}

	// 把時分秒歸0，變成日期。
	public static Date truncate(Date date) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return calendar.getTime();
		} catch (Exception e) {
			Log.warning("truncate(%s) failed.", format("yyyy-MM-dd HH:mm:ss", date));
			return date;
		}
	}

	public static Date addDay(Date date, int amount) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.DATE, amount);
			return calendar.getTime();
		} catch (Exception e) {
			Log.warning("addDay(%s, %s) failed.", format("yyyy-MM-dd HH:mm:ss", date), amount);
			return date;
		}
	}

	public static Date addMonth(Date date, int amount) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.MONTH, amount);
			return calendar.getTime();
		} catch (Exception e) {
			Log.warning("addMonth(%s, %s) failed.", format("yyyy-MM-dd HH:mm:ss", date), amount);
			return date;
		}
	}

	public static Date addYear(Date date, int amount) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.YEAR, amount);
			return calendar.getTime();
		} catch (Exception e) {
			Log.warning("addYear(%s, %s) failed.", format("yyyy-MM-dd HH:mm:ss", date), amount);
			return date;
		}
	}

	public static Date toDate(int year, int month, int date) {
		return toTime(year, month, date, 0, 0, 0);
	}

	public static Date toTime(int year, int month, int date, int hour, int minute, int second) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month - 1, date, hour, minute, second);
			return calendar.getTime();
		} catch (Exception e) {
			Log.warning("toTime(%s,%s,%s,%s,%s,%s) failed.", year, month, date, hour, minute, second);
			return null;
		}
	}

	// 比對常用的英文日期格式，比對不出來就回傳null。
	public static Date parse(String str) {
		if (str == null) {
			return null;
		}
		// 先判斷文字裡的時間 hh:mm:ss。
		// 在判斷三個數字，是'年月日'或'日月年'或'月日年'。
		// 如果月是英文，很可能日在年前面。如果月是數字，很可能年在日前面。
		try {
			int year = 0, month = 0, day = 0;
			int hour = 0, minute = 0, second = 0, ms = 0;
			long offset = 0;
			Calendar calendar = Calendar.getInstance();
			str = str.trim();
			if (Regex.matches(str, "\\d+")) {
				if (str.length() == 8) {
					year = Integer.parseInt(str.substring(0, 4));
					month = Integer.parseInt(str.substring(4, 6));
					day = Integer.parseInt(str.substring(6, 8));
				} else if (str.length() == 6) {
					year = Integer.parseInt(str.substring(0, 2));
					month = Integer.parseInt(str.substring(2, 4));
					day = Integer.parseInt(str.substring(4, 6));
				} else if (str.length() == 4) {
					year = Integer.parseInt(str);
					month = 1;
					day = 1;
				} else if (str.length() > 8) {
					return new Date(Long.parseLong(str));
				}
			} else if (Regex.matches(str, "\\d\\d\\d\\d.\\d\\d.\\d\\d")) {
				year = Integer.parseInt(str.substring(0, 4));
				month = Integer.parseInt(str.substring(5, 7));
				day = Integer.parseInt(str.substring(8, 10));
			} else if (Regex.matches(str, "\\d\\d.\\d\\d.\\d\\d\\d\\d")) {
				day = Integer.parseInt(str.substring(0, 2));
				month = Integer.parseInt(str.substring(3, 5));
				year = Integer.parseInt(str.substring(6, 10));
			} else {
				str = str.toUpperCase();
				boolean hasTime = false;
				boolean isDayBeforeYear = false;
				Pattern pattern = Regex.pattern(
						"(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?(?:\\.(\\d+))?[^\\d+-]*([+-]\\d\\d(?::?\\d\\d)?)?");
				Matcher matcher = pattern.matcher(str);
				String s;
				if (matcher.find()) {
					hasTime = true;
					hour = Integer.parseInt(matcher.group(1));
					minute = Integer.parseInt(matcher.group(2));
					s = matcher.group(3);
					second = s == null ? 0 : Integer.parseInt(s);
					s = matcher.group(4);
					ms = s == null ? 0 : Integer.parseInt(s.length() > 3 ? s.substring(0, 3) : s);
					s = matcher.group(5); // +8、+08、+0800、+8:00、+08:00
					str = str.substring(0, matcher.start()) + " " + str.substring(matcher.end());
					if (s != null && s.length() > 0) {
						int offsetHour = 0, offsetMinute = 0;
						boolean minus = s.charAt(0) == '-';
						s = s.substring(1);
						int i = s.indexOf(':');
						if (i != -1) {
							offsetHour = Integer.parseInt(s.substring(0, i));
							offsetMinute = Integer.parseInt(s.substring(i + 1));
						} else if (s.length() <= 2) {
							offsetHour = Integer.parseInt(s);
						} else if (s.length() == 4) {
							offsetHour = Integer.parseInt(s.substring(0, 2));
							offsetMinute = Integer.parseInt(s.substring(2, 4));
						}
						// TimeUnit.HOURS.toMillis(1) = 3600000
						// TimeUnit.MINUTES.toMillis(1) = 60000
						// TimeUnit.HOURS.toMillis(offsetHour) + TimeUnit.MINUTES.toMillis(offsetMinute)
						offset = (offsetHour * 3600000L) + (offsetMinute * 60000L);
						if (minus) {
							offset = -offset;
						}
						offset = offset - TimeZone.getDefault().getRawOffset();
					} else {
						int i = str.indexOf("GMT");
						if (i == -1) {
							i = str.indexOf("UTC");
						}
						if (i != -1) {
							offset = -TimeZone.getDefault().getRawOffset();
							str = str.substring(0, i) + str.substring(i + 3);
						}
					}
					if (hour > 0 && hour < 12 && str.indexOf("PM") != -1) {
						hour += 12;
					} else if (hour == 12 && str.indexOf("AM") != -1) {
						hour = 0;
					}

				}
				pattern = Regex.pattern("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\w*");
				matcher = pattern.matcher(str);
				if (matcher.find()) {
					month = month(matcher.group(1));
					isDayBeforeYear = false; // 如果月是英文，很可能日在年前面。
					str = str.substring(0, matcher.start()) + " " + str.substring(matcher.end());
				}
				String[] dateStrs = Regex.findAll(str, "(\\d+)");
				int length = dateStrs.length;
				int[] nums = new int[length];
				for (int i = 0; i < length; i++) {
					nums[i] = Integer.parseInt(dateStrs[i]);
				}
				// 先比對year
				for (int i = 0; i < length; i++) {
					if (nums[i] > 31) {
						year = nums[i];
						length--;
						for (int j = i; j < length; j++) {
							nums[j] = nums[j + 1];
						}
						if (i != 0) {
							isDayBeforeYear = true;
						}
						break;
					}
				}
				for (int i = 0; i < length; i++) {
					if (isDayBeforeYear) {
						if (day == 0 && nums[i] <= 31) {
							day = nums[i];
						} else if (month == 0 && nums[i] <= 12) {
							month = nums[i];
						} else if (year == 0) {
							year = nums[i];
						}
					} else {
						if (year == 0) {
							year = nums[i];
						} else if (month == 0 && nums[i] <= 12) {
							month = nums[i];
						} else if (day == 0 && nums[i] <= 31) {
							day = nums[i];
						}
					}
				}
				if (hasTime && dateStrs.length == 0 && year == 0 && month == 0 && day == 0) {
					year = calendar.get(Calendar.YEAR);
					month = calendar.get(Calendar.MONTH);
					day = calendar.get(Calendar.DAY_OF_MONTH);
				}
				if (year == 0 && month != 0 && day != 0) {
					year = calendar.get(Calendar.YEAR);
				} else if (year != 0 && month != 0 && day == 0) {
					day = 1;
				}
			}
			// System.out.print(String.format("[%s-%s-%s %s:%s:%s.%s %s] ", year, month,
			// day, hour, minute, second, ms, TimeUnit.MILLISECONDS.toHours(offset)));
			if (year != 0) {
				calendar.set(year, month - 1, day, hour, minute, second);
				calendar.set(Calendar.MILLISECOND, ms);
				if (offset != 0) {
					calendar.add(Calendar.MILLISECOND, (int) offset);
				}
				return calendar.getTime();
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return null;
	}

	private static int month(String monthStr) {
		switch (monthStr.toUpperCase()) {
		case "JAN":
			return 1;
		case "FEB":
			return 2;
		case "MAR":
			return 3;
		case "APR":
			return 4;
		case "MAY":
			return 5;
		case "JUN":
			return 6;
		case "JUL":
			return 7;
		case "AUG":
			return 8;
		case "SEP":
			return 9;
		case "OCT":
			return 10;
		case "NOV":
			return 11;
		case "DEC":
			return 12;
		default:
			return 0;
		}
	}

}
