# javautil

常用的java util工具集合，可直接把需要用到的class貼到專案裡使用即可。  

## [AutoBind、Reflect]
只要把宣告物件變數時加上@AutoBind，再使用Reflect.autoBind(paramGetter, obj)或Args.autoBind(obj)，就能自動把obj裡的變數綁訂對應的值。  

可參考範例
```
//test_entity/Person.java
public class Person {
    @AutoBind
    String name;
    @AutoBind
    int age;
}

// How to use
HashMap<String, String> map = new HashMap<>();
map.put("name", "Jason");
map.put("age", "18");
Person person = new Person();
Reflect.autoBind(map, person);
System.out.printf("name:%s, age:%d.\n", person.name, person.age);
```

## [Config]
只要新增同名的cfg檔，不用其他設定，直接呼叫Config.get()就能直接得到對應的設定。  

使用步驟：  
1.建立和jar檔同名的cfg檔案，放到jar檔相同的目錄或專案目錄。  
2.Config.setCategory();  
3.直接呼叫Config.get(param)取得相關設定的值。  

cfg檔案格式範例如下  
```
GlobalParam1=XXX
GlobalParam2=YYY

# Use number sign (#) to indicate that this line of text is a comment

[CategoryA]
param1=aaa
param2=apple
This is an apple blabla...

[CategoryB]
param1=bbb
param2=banana
```
設定Config.setCategory("CategoryA")後，呼叫Config.get("param1")會得到aaa。  
設定Config.setCategory("CategoryB")後，呼叫Config.get("param1")會得到bbb。  

不是等號格式(param=value)的文字會統一存成list，  
例如在設定CategoryA後，呼叫Config.getOtherLines()可以取得有字串「This is apple blabla...」的list。  

## [Args]
只要在程式main函式一開始執行Args.load(args)，之後就能使用Args.get()來取得指令參數。  
指令格式：
```
java -jar javautil.jar <cfg_category> -paramA valueA -paramB valueB...
```
第一參數，可以是<cfg_category>，也可以省略。  
為了方便，在Args.get("paramA")時，會優先檢查指令有沒有包含-paramA該選項，沒有的話會去查找cfg有沒有設定。  

可以搭配AutoBind機制一起使用。
```
java -jar javautil.jar -name Sam -age 18 ...
```
```
Person person = new Person();
Args.autoBind(person);
```
或者可以把變數設定在cfg裡。
```
//javautil.cfg
[Person1]
name=Harry Potter
age=22
birthday=2020/01/02
```
```
java -jar javautil.jar Person1
```
```
Person person = new Person();
Args.autoBind(person);
```

## [Log]
簡單方便的Log類別，直接呼叫Log.info()即可使用。隨時使用，不用每個類別開頭都需要額外宣告宣告一行log變數。  
一開始可以用Log.setLogPath()設定log名稱。  
每次呼叫info()、warning()、error()時，都會getStackTrace()一下以紀錄呼叫的類別和函式名稱，簡單暴力樸實無華。  
考慮效能問題，可以設定InfoWriteClassMethod=false讓info()不必輸出函式名稱。  

## [Dates]
強大方便的日期處理工具  
Dates.parse() 可以判斷各種常用格式的時間  
```
System.out.println(Dates.parse("2020/01/02"));
System.out.println(Dates.parse("2021-03-05 07:09:11"));
System.out.println(Dates.parse("06 Apr 2022"));
System.out.println(Dates.parse("20220330")); //8個數字判斷日期
System.out.println(Dates.parse("1649174400000")); //超過8個數字判斷1970年後的毫秒
```
Dates.replaceSymbol 可以把FILENAMEyyyymmdd.txt轉成FILENAME20220330.txt。  
Dates.format 可以幫忙用SimpleDateFormat解析固定格式的日期，並把建立的SimpleDateFormat暫存起來，方便持續使用，增加效率。

## [Paths]
方便的Path處理工具  
Paths.get(String folder, String filename) 可以取代預設的Paths，優點是不用擔心filename是不是絕對路徑，或者folder尾巴有沒有忘記加斜線。  
Paths.preparePath() 確認目前路徑是否存在並建立，如果已有同名檔案會幫舊檔案rename，避免覆蓋。  
Paths.jarFolder() 返回目前jar的路徑。  
Paths.getSimpleName() 取得不含副檔名的檔名  
Paths.getExtension() 取得副檔名  

## [Regex]
方便的正則表示法處理工具 
把Pattern、Matcher常用的語法包成函式，方便呼叫，並且會把編譯過的Pattern暫存起來，減少重新編譯的時間。
```
String regex = "\\d+";
String input = "0123ABCD";

//before
Pattern pattern = Pattern.compile(regex);
Matcher matcher = pattern.matcher(input);
String find = matcher.find() ? matcher.group() : null;

//after
String find = Regex.find(input, regex);
```

## [Strings]
方便的字串處理工具  
Strings.isEmpty()和Strings.notEmpty() 檢查字串是否為null或""。  
Strings.center()、Strings.leftPad()和Strings.rightPad() 可以用來填充字串，進行文字排版。

Strings.split()  
Java雖已有內建String.split()，但此函式有額外的特點。  
特點1: 若analyzeEscapeChar=true，會自動轉換跳脫字元，且雙引號內的文字不會被切割。  
特點2: 當分析字串str="|a|b||c"時，str.split()的結果為[a,b,c]，首尾和重複的delimiter會被省略。  
       Strings.split(str)依照ignoreEmpty的false或true，結果為[,a,b,,c]或[a,b,c]。  
