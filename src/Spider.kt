/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 21:09
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.spider


import com.github.rwsbillyang.spider.news.*
import com.github.rwsbillyang.spider.video.DouYinSpider
import com.github.rwsbillyang.spider.video.KuaiShouSpider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern


object Spider {
    val UAs_PC = 0
    val UAs_Mobile = 1
    //像今日头条，不同的浏览器重定向到不同的域名。如视频在pc上重定向到西瓜视频，
    // 微头条重定向到：https://m.toutiaocdn.com/i1692743955618829/ -> https://www.toutiao.com/w/i1692743955618829/
    //微信浏览器中则不重定向
    val UAs_WX = 2

    val uas = arrayOf(
        arrayOf( //UAs_PC
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 5.1; rv:6.0.2) Gecko/20100101 Firefox/6.0.2"
        ),
        arrayOf( //UAs_Mobile
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (Linux; Android 7.1.1; PRO 6s Build/NMF26O; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/045008 Mobile Safari/537.36 MMWEBID/2921",
            "Mozilla/5.0 (Linux; Android 9; COR-AL10 Build/HUAWEICOR-AL10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/045008 Mobile Safari/537.36 MMWEBID/1039",
        ),
        arrayOf(//UAs_WX
            "Mozilla/5.0 (Linux; Android 8.1.0; MI PLAY Build/O11019; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045514 Mobile Safari/537.36 MMWEBID/3350 MicroMessenger/7.0.16.1700(0x2700103E) Process/tools WeChat/arm32 NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 10; Mi 10 Pro Build/QKQ1.191117.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/71.0.3578.99 Mobile Safari/537.36 MMWEBID/3752 MicroMessenger/7.0.15.1680(0x27000F35) Process/tools WeChat/arm64 NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 9; Redmi Note 8 Build/PKQ1.190616.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.62 XWEB/2759 MMWEBSDK/201201 Mobile Safari/537.36 MMWEBID/4325 MicroMessenger/8.0.1840(0x28000037) Process/toolsmp WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 8.1.0; MI PLAY Build/O11019; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045513 Mobile Safari/537.36 MMWEBID/3350 MicroMessenger/8.0.1.1840(0x2800013B) Process/tools WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 8.1.0; MI PLAY Build/O11019; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/71.0.3578.99 Mobile Safari/537.36 MMWEBID/3350 MicroMessenger/8.0.1.1840(0x2800013B) Process/tools WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 9; vivo X21A Build/PKQ1.180819.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045520 Mobile Safari/537.36 MMWEBID/4470 MicroMessenger/8.0.1.1841(0x2800015C) Process/tools WeChat/arm32 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 MicroMessenger/7.0.3(0x17000321) NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/7.0.12(0x17000c30) NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/6.5.14 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (Linux; Android 10; ZTE A2121 Build/QKQ1.200913.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045513 Mobile Safari/537.36 MMWEBID/8141 MicroMessenger/8.0.1.1841(0x2800015D) Process/tools WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 10; SEA-AL10 Build/HUAWEISEA-AL10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.62 XWEB/2759 MMWEBSDK/201201 Mobile Safari/537.36 MMWEBID/663 MicroMessenger/8.0.1.1841(0x2800015D) Process/toolsmp WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (Linux; Android 9; BND-AL00 Build/HONORBND-AL00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045513 Mobile Safari/537.36 MMWEBID/9024 MicroMessenger/8.0.1.1841(0x2800015D) Process/tools WeChat/arm64 Weixin NetType/4G Language/zh_CN ABI/arm64"
        )
    )

    const val RET = "ret"
    const val KO = "ko"
    const val MSG = "msg"
    const val OK = "ok"


    const val TITLE = "title"
    const val BRIEF = "brief"
    const val IMGURL = "imgUrl"
    const val CONTENT = "content"

    const val LINK = "link"

    const val USER = "user"
    const val USER2 = "user2"

    const val TAG = "tag"
    const val OGURL = "ogurl"

    const val VIDEO_COVER = "video_cover"
    const val VIDEO = "video"
    const val MUSIC = "music"

    fun parse(url: String): Map<String, String?> {
        val spider = create(url)
        if (spider == null) {
            val map = mutableMapOf<String, String?>()
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "暂只支持微信公众号文章，实验性支持：今日头条、抖音短视频、快手短视频、百度和163，请换个链接试试吧" //快手短视频、今日头条、
            return map
        }
        val isInvalid: Boolean = Pattern.matches(spider.regPattern, url)
        if (!isInvalid) {
            val map = mutableMapOf<String, String?>()
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = spider.errMsg
            return map
        }
        return spider.doParse(url)
    }

    private fun create(url: String): ISpider? {
        return if (url.contains("mp.weixin.qq.com")) {
            WechatArticleSpider()
        }
        else if(url.contains(".toutiao")){
            ToutiaoSpider()
        }
        else if (url.contains(".kuaishou")) {
            KuaiShouSpider()
        }else if(url.contains("douyin.com")){
            DouYinSpider()
        }
        else if (url.contains("baidu.com")) {
            BaiduSpider()
        }
        else if (url.contains("163.com")) {
            if(url.contains("//3g.163.com")) Spider3G163()
            else if(url.contains("//c.m.163.com")) SpiderCM163()
            else Spider3G163()
        }
        else {
            null
        }
    }
    /**
     * 检测url是否有效 有效返回true，失效返回false
     * */
    fun verifyUrl(urlStr: String?): Boolean {
        if(urlStr.isNullOrBlank()) return  false
        return try {
            val url = URL(urlStr)
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3 * 1000
            conn.requestMethod = "HEAD"
            conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: MalformedURLException) {
            //e.printStackTrace()
            println("MalformedURLException: url=$urlStr")
            false
        } catch (e: IOException) {
            println("IOException: url=$urlStr")
            false
        }
    }
}
