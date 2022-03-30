package tw.com.jinnboy.javautil.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {

	private Regex() {
	}

	private static HashMap<String, Pattern> patterns = new HashMap<>();

	public static Pattern pattern(String regex) {
		return pattern(regex, 0);
	}

	// 把regex對應的pattern暫存起來，才不用每次重新compile，空間換取時間。
	// flags最常用的是Pattern.CASE_INSENSITIVE
	public static Pattern pattern(String regex, int flags) {
		String key = flags == 0 ? regex : flags + "#@#" + regex;
		Pattern pattern = patterns.get(key);
		if (pattern == null) {
			pattern = Pattern.compile(regex, flags);
			patterns.put(key, pattern);
		}
		return pattern;
	}

	public static boolean matches(CharSequence input, String regex) {
		return pattern(regex).matcher(input).matches();
	}

	public static boolean matches(CharSequence input, String regex, int flags) {
		return pattern(regex, flags).matcher(input).matches();
	}

	public static String find(CharSequence input, String regex) {
		return find(input, regex, 0);
	}

	public static String find(CharSequence input, String regex, int flags) {
		Matcher m = pattern(regex, flags).matcher(input);
		if (m.find()) {
			return m.group();
		} else {
			return null;
		}
	}

	public static String[] findAll(CharSequence input, String regex) {
		return findAll(input, regex, 0);
	}

	public static String[] findAll(CharSequence input, String regex, int flags) {
		Matcher m = pattern(regex, flags).matcher(input);
		ArrayList<String> list = new ArrayList<>();
		while (m.find()) {
			list.add(m.group());
		}
		return list.toArray(new String[list.size()]);
	}

	public static String[] groups(CharSequence input, String regex) {
		return groups(input, regex, 0);
	}

	public static String[] groups(CharSequence input, String regex, int flags) {
		Matcher m = pattern(regex, flags).matcher(input);
		if (m.find()) {
			int groupCount = m.groupCount();
			String[] groups = new String[groupCount + 1];
			for (int i = 0; i < groups.length; i++) {
				groups[i] = m.group(i);
			}
			return groups;
		}
		return new String[0];
	}

	public static String replaceAll(CharSequence input, String regex, String replacement) {
		return pattern(regex).matcher(input).replaceAll(replacement);
	}

	public static String replaceAll(CharSequence input, String regex, int flags, String replacement) {
		return pattern(regex, flags).matcher(input).replaceAll(replacement);
	}

	public static void test(CharSequence input, String regex) {
		test(input, regex, 0);
	}

	public static void test(CharSequence input, String regex, int flags) {
		System.out.println("========== Regex Test ==========");
		System.out.println(regex);
		System.out.println(input);
		Pattern pattern = pattern(regex, flags);
		Matcher matcher = pattern.matcher(input);
		int find = 0;
		while (matcher.find()) {
			System.out.println("---------- find " + (++find) + " ----------");
			int groupCount = matcher.groupCount();
			System.out
					.println(String.format("[index=%s-%s; groupCount=%s]", matcher.start(), matcher.end(), groupCount));
			for (int i = 0; i <= groupCount; i++) {
				System.out.println(String.format("group(%s): %s", i, matcher.group(i)));
			}
		}
	}
}
