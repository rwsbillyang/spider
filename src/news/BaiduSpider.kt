/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-24 17:34
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

package com.github.rwsbillyang.spider.news


import com.github.rwsbillyang.spider.SeleniumSpider
import com.github.rwsbillyang.spider.Spider
import com.github.rwsbillyang.spider.utils.HtmlImgUtil

import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration

class BaiduSpider(binary: String? = null) : SeleniumSpider(binary)  {
    private val log: Logger = LoggerFactory.getLogger("BaiduSpider")

    override val regPattern = "http(s)?://(baijiahao|news|mbd)\\.baidu\\.com/\\S+"
    override val errMsg = "链接应为：baijiahao.baidu.com 或 news.baidu.com"

    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        log.info("parse url=$url")

         if(url.contains("baijiahao.") || url.contains("mbd.")){
            parseBaiJiaHao(url, map)
        }else if(url.contains("news")){
            parseBaiDuNews(url, map)
        }else{
            log.warn("not support url=$url")
            map[Spider.MSG] = "不支持链接，请使用baijiahao.baidu.com 或 news.baidu.com"
            map[Spider.RET] = Spider.KO
        }
        return map
    }
    private fun parseBaiJiaHao(url: String, map: MutableMap<String, String?>) {
        val driver: WebDriver = ChromeDriver(chromeOptions)
        try {
            driver.get(url)// 目标地址
            val contentElem: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                .until { driver.findElement(By.cssSelector("div.mainContent")) }

            map[Spider.LINK] = driver.currentUrl
            map[Spider.TITLE] = driver.title
            map[Spider.USER] = driver.findElement(By.cssSelector("a.authorName")).text?:"百家号"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
            if(content!= null)
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK
        } catch(e: NoSuchElementException){
            log.error("NoSuchElementException: ${e.message},driver.currentUrl=${driver.currentUrl}")
        }catch(e: TimeoutException){
            log.error("TimeoutException: ${e.message},driver.currentUrl=${driver.currentUrl}")
        }catch (e: IOException) {
            log.error("IOException: ${e.message},url=$url")
            map[Spider.MSG] = "获取内容时IO错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }catch (e: Exception){
            e.printStackTrace()
            log.error("Exception: ${e.message}, url=$url")
            map[Spider.MSG] = "获取内容时出现错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }finally {
            driver.close()
        }

    }

    private fun parseBaiDuNews(url: String, map: MutableMap<String, String?>) {
        val driver: WebDriver = ChromeDriver(chromeOptions)
        try {
            driver.get(url)// 目标地址
            val contentElem: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                .until { driver.findElement(By.cssSelector("div#newsDetailContent")) }

            map[Spider.LINK] = driver.currentUrl
            map[Spider.TITLE] = driver.title
            map[Spider.USER] = driver.findElement(By.cssSelector("div.header-info>span")).text?:"百度新闻"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
            if(content!= null)
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK
        } catch (e: IOException) {
            log.error("IOException: ${e.message},url=$url")
            map[Spider.MSG] = "获取内容时IO错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }catch (e: Exception){
            e.printStackTrace()
            log.error("Exception: ${e.message}, url=$url")
            map[Spider.MSG] = "获取内容时出现错误，请稍后再试"
            map[Spider.RET] = Spider.KO
        }finally {
            driver.close()
        }
    }
}

fun main(args: Array<String>) {
    BaiduSpider("/Users/bill/git/youke/server/app/zhiKe/chromedriver")
        .doParse("https://mbd.baidu.com/newspage/data/landingshare?p_from=7&n_type=-1&context=%7B%22nid%22%3A%22news_9367297194102527383%22%7D")
        .forEach {
            println("${it.key}=${it.value}")
        }
}