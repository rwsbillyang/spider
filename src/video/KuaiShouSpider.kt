/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-23 11:21
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

package com.github.rwsbillyang.spider.video


import com.github.rwsbillyang.spider.Spider
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.IOException


/**
 * 动态网页，即使用htmlunit，也未成功，因htmlunit中js执行出错
 * 改为selenium
 *
 * The path to the driver executable must be set by the webdriver.chrome.driver system property;
 * for more information, see https://github.com/SeleniumHQ/selenium/wiki/ChromeDriver.
 * The latest version can be downloaded from http://chromedriver.storage.googleapis.com/index.html
 *
 * 其它参考实现：
 * https://github.com/bajingxiaozi/video_parse/blob/master/src/main/java/com/xyf/video/parse/KuaishouLinkParse.java
 * https://docs.tenapi.cn/kuaishou.html
 * */
class KuaiShouSpider(binary: String? = null) : VideoSpider() {
    override val regPattern = "[^x00-xff]*\\s*http(s)?://(\\w|-)+\\.kuaishou(app)?\\.com/\\S+\\s*[^x00-xff]*"
    override val errMsg = "请确认链接是否包含： https://v.kuaishou.com/ 或 https://v.kuaishouapp.com/"

    init {
        System.setProperty("webdriver.chrome.driver", binary?:"./chromedriver") // 必须加入
    }
    private val chromeOptions = ChromeOptions().apply {
        setHeadless(true) //已包含addArguments("--disable-gpu")
        addArguments("--user-agent="+Spider.UAs[2])
        addArguments("--blink-settings=imagesEnabled=false") //禁用图片加载
        addArguments("--incognito")
        addArguments("--window-size=400,300")
        addArguments("--disable-dev-shm-usage")
        addArguments("--disable-extensions")
        addArguments("--lang=zh-CN")
        addArguments("--disable-images")
        addArguments("--single-process")
        addArguments ("--no-sandbox")

        //setBinary(binary?:"./chromedriver") //not work
    }


    override fun decodeHttpUrl(originUrl: String): String {
        val start = originUrl.indexOf("http")
        val end = originUrl.indexOf("复制")
        return if(end < 0) originUrl.substring(start).trim()
        else originUrl.substring(start, end).trim()
    }


    //https://c.kuaishou.com/fw/photo/3xmtv34g7te766m
    //https://video.kuaishou.com/short-video/3xmtv34g7te766m
    //https://c.kuaishou.com/fw/photo/3xmtv34g7te766m?fid=364977367&cc=share_copylink&shareMethod=TOKEN&docId=0&kpn=KUAISHOU&subBiz=PHOTO&photoId=3xmtv34g7te766m&shareId=174934779986&shareToken=X-9PkGU53YiW8AYq_A&shareResourceType=PHOTO_OTHER&userId=3x3ns26k8ma3mbw&shareType=1&et=1_i%2F0_unknown0&groupName=&appType=1&shareObjectId=28588605066&shareUrlOpened=0&timestamp=1589543938285
    // originUrl: #汪星人 #宠物避障挑战 https://v.kuaishou.com/5xXNiL 复制此链接，打开【快手App】直接观看！
    //https://video.kuaishou.com/short-video/3x427dag2cd4je6?authorId=3xc4zhidhjika4a&streamSource=find&area=homexxbrilliant
    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        val url2 = decodeHttpUrl(url.replace("kuaishouapp.com","kuaishou.com")).split('?').first()

        log.info("originUrl=$url, url=$url2")

        val driver: WebDriver = ChromeDriver(chromeOptions)
        try {
            driver.get(url2)// 目标地址
            val video: WebElement = WebDriverWait(driver, 30)
                .until { d -> driver.findElement(By.tagName("video")) }

            map[Spider.VIDEO_COVER] = video.getAttribute("poster")?.split("&")?.firstOrNull()
            map[Spider.VIDEO] = video.getAttribute("src")?.split("?")?.firstOrNull()
            map[Spider.TITLE] = video.getAttribute("alt")?.split("#")?.firstOrNull()

            map[Spider.IMGURL] = driver.findElement(By.cssSelector("link[rel=\"image-src\"]")).getAttribute("href")
            map[Spider.USER] = driver.findElement(By.cssSelector("div.auth-name")).text?:"KuaiShou"

            map[Spider.LINK] = url2
            map[Spider.RET] = Spider.OK
            map[Spider.MSG] = "恭喜，解析成功"

            return map
        } catch (e: IOException) {
            log.error("${e.message},originUrl=$url, url=$url2")
            map[Spider.MSG] = "获取内容时IO错误，请稍后再试"
        }catch (e: Exception){
            log.error("${e.message},originUrl=$url, url=$url2")
            map[Spider.MSG] = "获取内容时出现错误，请稍后再试"
        }
        finally {
            driver.close()
        }

        map[Spider.RET] = Spider.KO
        map[Spider.MSG] = "获取内容时网络超时，请重试"
        return map
    }
}

//这个家，我怕是回不去了！#超级段子手计划 https://v.kuaishou.com/bI3JFM 复制此消息，打开【快手】直接观看！
fun main(args: Array<String>) {
    KuaiShouSpider("/Users/bill/git/youke/server/app/mainApp/chromedriver")
        .doParse("这个家，我怕是回不去了！#超级段子手计划 https://v.kuaishou.com/bI3JFM 复制此消息，打开【快手】直接观看！")
        .forEach {
            println("${it.key}=${it.value}")
        }
}
