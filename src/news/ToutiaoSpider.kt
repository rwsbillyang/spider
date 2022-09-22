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

import com.github.rwsbillyang.spider.ChromeDriverServiceWrapper
import com.github.rwsbillyang.spider.ISpider
import com.github.rwsbillyang.spider.Spider
import com.github.rwsbillyang.spider.utils.HtmlImgUtil
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.MalformedURLException
import java.time.Duration


class ToutiaoSpider(private val webDriver: WebDriver): ISpider {
    private val log: Logger = LoggerFactory.getLogger("ToutiaoSpider")
    override val regPattern = "http(s)?://(m|www)\\.(toutiao|toutiaocdn|toutiaoimg)\\.(com|cn|net)/\\S+"
    override val errMsg = "可能不是头条链接"



    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        log.info("parse url=$url")

        //val driver: WebDriver = ChromeDriver(chromeOptions)
        try {
            webDriver.get(url)// 目标地址
            val newUrl = webDriver.currentUrl
            map[Spider.LINK] = newUrl
            if(newUrl.contains("/article/")){
                val article: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                    .until { webDriver.findElement(By.tagName("article")) }
                var content = article.getAttribute("innerHTML")

                //某些链接中会包含视频，需点击后才能出现video标签，https://m.toutiao.com/article/7133606462558896647/?app=news_article&timestamp=1663602881&use_new_style=1&req_id=20220919235440010209157026040F8ADF&group_id=7133606462558896647&share_token=187941c2-9f53-4ba6-879c-a5c3875c6003&tt_from=copy_link&utm_source=copy_link&utm_medium=toutiao_android&utm_campaign=client_share&source=m_redirect&upstream_biz=toutiao_pc
                webDriver.findElements(By.cssSelector("div.tt-video-box"))?.forEach {
                    val c = it.getAttribute("outerHTML")
                    it.click() //点击后才出现video信息
                    val video: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                        .until { webDriver.findElement(By.cssSelector("video")) }

                    val src = video.getAttribute("src")
                    val v = "<p><video src=\"$src\" controls loop autoPlay=\"false\" preload=\"metadata\" width=\"100%\"  playsInline webkit-playsinline x5-playsinline x6-playsinline x5-video-player-fullscreen=\"false\"  x5-video-player-type=\"h5\" x-webkit-airplay=\"allow\"></p>"
                    //log.info("c=$c, v=$v")
                    content = content.replace(c, v)
                }

                map[Spider.CONTENT] = content
                if(content!= null)
                    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.TITLE] = webDriver.findElement(By.cssSelector("h1"))?.text
                map[Spider.BRIEF] = webDriver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

                map[Spider.USER] = webDriver.findElement(By.cssSelector("div.author-info>div.author-info-name")).text?:"头条"

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/video/")){
                val video: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                    .until { webDriver.findElement(By.tagName("video")) }

                map[Spider.TITLE] = webDriver.findElement(By.cssSelector("h1.video-title"))?.text
                map[Spider.VIDEO] = video.findElements(By.tagName("source"))?.firstOrNull()?.getAttribute("src")
                map[Spider.VIDEO_COVER] = webDriver.findElements(By.tagName("xg-poster"))?.firstOrNull()?.getAttribute("style")?.substringAfter("url(\"")?.removeSuffix("\");")

                map[Spider.USER] = webDriver.findElement(By.cssSelector("div.author-info-name"))?.text?:"头条"
                map[Spider.IMGURL] = webDriver.findElement(By.cssSelector("div.author-avatar>img"))?.getAttribute("src")

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/question/")){
                val ul: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                    .until { webDriver.findElement(By.cssSelector("ul.answerlist")) }

                map[Spider.TITLE] = webDriver.findElement(By.cssSelector("h1.title"))?.text
                val content = ul.getAttribute("outerHTML")
                map[Spider.CONTENT] = content
                map[Spider.USER] ="头条"
                if(content!= null)
                    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else if(newUrl.contains("/w/")){ //weitoutiao-content
                val article: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                    .until { webDriver.findElement(By.cssSelector("div.weitoutiao-content")) }
                var content = article.getAttribute("innerHTML")

                val imgs = webDriver.findElement(By.cssSelector("div.image-list"))
                    .findElements(By.tagName("div")).firstOrNull()?.getAttribute("outerHTML")?:"" //?.getAttribute("innerHTML")

                content += imgs
                //if(content!= null) //留给前端通过解析dom获取
                //    map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

                map[Spider.CONTENT] = content

                map[Spider.TITLE] = webDriver.title.removeSuffix("-今日头条")
                map[Spider.BRIEF] = webDriver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

                map[Spider.USER] = webDriver.findElement(By.cssSelector("div.author-info>div.author-info-name")).text?:"头条"

                map[Spider.MSG] = "恭喜，解析成功"
                map[Spider.RET] = Spider.OK
            }else{
                log.warn("not support toutiao url=$url")
                map[Spider.MSG] = "暂不支持此类型链接"
                map[Spider.RET] = Spider.KO
            }

        }catch(e: NoSuchElementException){
            log.error("NoSuchElementException: ${e.message},driver.currentUrl=${webDriver.currentUrl}")
        }catch(e: TimeoutException){
            log.error("TimeoutException: ${e.message},driver.currentUrl=${webDriver.currentUrl}")
        }catch (e: MalformedURLException){
            log.error("MalformedURLException: ${e.message},url=$url")
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
            webDriver.close()
        }

        return map
    }
}
