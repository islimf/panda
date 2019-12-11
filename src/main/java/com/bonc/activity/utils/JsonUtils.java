package com.bonc.activity.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

public class JsonUtils {
    private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static HashMap<String, String> resultCodeMap;

    public static Map<String, Object> dealJsonParam(String s) {
        logger.info("JsonParam:\t{}", s);
        return JSONObject.toJavaObject(JSON.parseObject(s), Map.class);
    }

    public static Map<String, Object> dealJsonParam1(String s) {

        return JSONObject.toJavaObject(JSON.parseObject(s), Map.class);
    }


    public static boolean checkJsonParam(Map<String, Object> map, Set<String> set) {
        return map.keySet().containsAll(set);
    }

    public static boolean checkJsonParam(Map<String, Object> map, String param) {
        HashSet<String> set = new HashSet<>();
        for (String str : param.split(","))
            set.add(str);
        return map.keySet().containsAll(set);
    }

    public static JSONObject getExceptionJSONObject(String resultCode) {
        JSONObject o = new JSONObject();
        o.put("resultCode", resultCode);
        return o;
    }


    public static String getResponse(JSONArray a) {
        return toJson(a);
    }

    public static String getResponse(JSONObject a) {
        if (a.containsKey("resultCode")) {
            return toExceptionJson(a);
        }
        return toJson(a);
    }

    public static String toJson(Object a) {
        JSONObject o = new JSONObject();
        if (a != null) {
            logger.info("{}", a.toString());
        }
        o.put("resultCode", "0000");
        o.put("resultDesc", "");
        o.put("resultData", a);
        return o.toJSONString();
    }

    private static String toExceptionJson(JSONObject a) {
        JSONObject o = a;
        o.put("resultDesc", resultCodeMap.get(o.get("resultCode")));
        o.put("resultData", "");
        return o.toJSONString();
    }

    public static List<String> stringToList(String strs) {
        String str[] = strs.split(",");
        return Arrays.asList(str);
    }

    public static Object textToJson(String text) {
        Object objectJson = JSON.parse(text);
        return objectJson;
    }

    public static HashMap stringToHashMap(String s) {
        Map m = JSONObject.parseObject(s);
        HashMap<String, String> returnMap = new HashMap<String, String>();
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Entry) it.next();
            returnMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return returnMap;
    }

    static {
        resultCodeMap = new HashMap<>();
        Properties p = new Properties();
        InputStream i = PropertiesUtil.class.getClassLoader().getResourceAsStream("result-code.properties");
        try {
            p.load(i);
            for (Object key : p.keySet()) {
                resultCodeMap.put(key.toString(), p.get(key).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将map转化为string
     *
     * @param m
     * @return
     */
    public static String collectToString(Map m) {
        String s = JSONObject.toJSONString(m);
        return s;
    }

    /**
     * json字符串转化为map
     *
     * @param s
     * @return
     */
    public static Map stringToCollect(String s) {
        Map m = JSONObject.parseObject(s);
        return m;
    }

    public static void main(String[] args) {

//        Map<String, String> paramMap = new HashMap<String, String>();
//        paramMap.put("token", "233");
//        paramMap.put("loginId", "test");
//        paramMap.put("appUrl", "http");
//        paramMap.put("appId", "root");
//        paramMap.put("msg", "");
//        paramMap.put("flowFlag", "01");
//        paramMap.put("statusFlag", "00");
//        paramMap.put("desc", "ceshi");
//        // 构造返回参数
//        Map<String, Object> returnMap = new HashMap<String, Object>();
//        returnMap.put("resultCode", "200");
//        returnMap.put("resultMsg", paramMap);
//        Map<String, Object> returnMap2 = new HashMap<String, Object>();
//        returnMap2.put("data", returnMap);
//        String returnStr = JSONUtil.collectToString(returnMap2);
//        System.out.println(returnStr);

        double money = 0;

        money = Double.valueOf("-1808.5");

        System.out.println("-----------money:" + String.valueOf(money));

    }


    public static String ok(Object data) {
        JSONObject o = new JSONObject();
        if (data != null) {
            logger.info("{}", data.toString());
        }
        o.put("resultCode", "0000");
        o.put("resultDesc", "success");
        o.put("resultData", data);
        return o.toJSONString();
    }

    public static String error(String resultDesc) {
        JSONObject o = new JSONObject();
        o.put("resultCode", "0001");
        o.put("resultDesc", resultDesc);
        return o.toJSONString();
    }


}