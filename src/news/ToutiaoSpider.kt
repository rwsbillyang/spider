/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-27 16:20
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


class ToutiaoSpider(binary: String? = null) : SeleniumSpider(binary)  {
    private val log: Logger = LoggerFactory.getLogger("ToutiaoSpider")
    override val regPattern = "http(s)?://(m|www)\\.(toutiao|toutiaocdn|toutiaoimg)\\.(com|cn)/\\S+"
    override val errMsg = "可能不是头条链接"



    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        log.info("parse url=$url")

        val driver: WebDriver = ChromeDriver(chromeOptions)
        try {
            driver.get(url)// 目标地址
            val newUrl = driver.currentUrl
            map[Spider.LINK] = newUrl
            if(newUrl.contains("/article/")){
                val article: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                    .until { driver.findElement(By.tagName("article")) }
                val content = article.getAttribute("innerHTML")
                map[Spider.CONTENT] = content
                if(content!= null)
                    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.TITLE] = driver.findElement(By.cssSelector("h1"))?.text
                map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

                map[Spider.USER] = driver.findElement(By.cssSelector("div.author-info>div.author-info-name")).text?:"头条"

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/video/")){
                val video: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                    .until { driver.findElement(By.tagName("video")) }

                map[Spider.TITLE] = driver.findElement(By.cssSelector("h1.video-title"))?.text
                map[Spider.VIDEO] = video.findElements(By.tagName("source"))?.firstOrNull()?.getAttribute("src")
                map[Spider.VIDEO_COVER] = driver.findElements(By.tagName("xg-poster"))?.firstOrNull()?.getAttribute("style")?.substringAfter("url(\"")?.removeSuffix("\");")

                map[Spider.USER] = driver.findElement(By.cssSelector("div.author-info-name"))?.text?:"头条"
                map[Spider.IMGURL] = driver.findElement(By.cssSelector("div.author-avatar>img"))?.getAttribute("src")

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/question/")){
                val ul: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                    .until { driver.findElement(By.cssSelector("ul.answerlist")) }

                map[Spider.TITLE] = driver.findElement(By.cssSelector("h1.title"))?.text
                val content = ul.getAttribute("outerHTML")
                map[Spider.CONTENT] = content
                map[Spider.USER] ="头条"
                if(content!= null)
                    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/w/")){ //weitoutiao-content
                val article: WebElement = WebDriverWait(driver, Duration.ofSeconds(timeOut))
                    .until { driver.findElement(By.cssSelector("div.weitoutiao-content")) }
                var content = article.getAttribute("innerHTML")

                val imgs = driver.findElement(By.cssSelector("div.image-list"))
                    .findElements(By.tagName("div")).firstOrNull()?.getAttribute("outerHTML")?:"" //?.getAttribute("innerHTML")

                content += imgs
                //if(content!= null)
                //    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.CONTENT] = content

                map[Spider.TITLE] = driver.title.removeSuffix("-今日头条")
                map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

                map[Spider.USER] = driver.findElement(By.cssSelector("div.author-info>div.author-info-name")).text?:"头条"

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else{
                log.warn("not support toutiao url=$url")
                map[Spider.MSG] = "暂不支持此类型链接"
                map[Spider.RET] = Spider.KO
            }

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

        return map
    }
}

fun main() {
    //由于IDEA中使用的server/build.gradle配置，kbson等需要jdk11,故需将spider/gradle.properties中jdk版本改为11
    //而在命令行下进行spider的库编译时，需要改成1.8版本，以生成支持jdk1.8及以上版本的库，否则将生成生成只支持11+的spider库
    ToutiaoSpider("/Users/bill/git/youke/server/app/zhiKe/chromedriver")
        .doParse("https://m.toutiao.com/w/1744012349973518?app=news_article&timestamp=1663669973&use_new_style=1&share_token=942703b5-294f-40ba-848a-e357a87ee8a7&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&upstream_biz=toutiao_pc&from_gid=1744001366265856&from_page_type=weitoutiao")
        .forEach {
            println("${it.key}=${it.value}")
        }
}
