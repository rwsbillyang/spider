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


import com.github.rwsbillyang.spider.ChromeDriverServiceWrapper
import com.github.rwsbillyang.spider.Spider
import com.github.rwsbillyang.spider.WebDriverClient
import com.github.rwsbillyang.spider.utils.HtmlImgUtil
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class BaiduSpider(uaIndex: Int = Spider.UAs_PC): WebDriverClient(uaIndex){

    override val regPattern = "http(s)?://(baijiahao|news|mbd)\\.baidu\\.com/\\S+"
    override val errMsg = "链接应为：baijiahao.baidu.com 或 news.baidu.com"


    override fun extract(url: String, map: MutableMap<String, String?>){
        if(url.contains("baijiahao.") || url.contains("mbd.")){
            if(uaIndex == Spider.UAs_PC){
                parseBaiJiaHaoByPc(map)
            }else{
                parseBaiJiaHao(map)
            }
        }else if(url.contains("news")){
            if(uaIndex == Spider.UAs_PC){
                parseBaiNewsByPc(map)
            }else{
                parseBaiDuNews(map)
            }
        }else{
            log.warn("not support url=$url")
            map[Spider.MSG] = "不支持链接，请使用baijiahao.baidu.com 或 news.baidu.com"
            map[Spider.RET] = Spider.KO
        }
    }

    private fun parseBaiJiaHao(map: MutableMap<String, String?>) {
            val contentElem: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                .until { webDriver.findElement(By.cssSelector("div.mainContent")) }

            map[Spider.LINK] = webDriver.currentUrl
            map[Spider.TITLE] = webDriver.title
            map[Spider.USER] = webDriver.findElement(By.cssSelector("a.authorName")).text?:"百家号"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
            if(content!= null)
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK
    }

    private fun parseBaiDuNews(map: MutableMap<String, String?>) {
            val contentElem: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                .until { webDriver.findElement(By.cssSelector("div#newsDetailContent")) }

            map[Spider.LINK] = webDriver.currentUrl
            map[Spider.TITLE] = webDriver.title
            map[Spider.USER] = webDriver.findElement(By.cssSelector("div.header-info>span")).text?:"百度新闻"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
            if(content!= null)
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK

    }
    private fun parseBaiJiaHaoByPc(map: MutableMap<String, String?>) {
            val contentElem: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                .until { webDriver.findElement(By.cssSelector("div.index-module_articleWrap_2Zphx ")) }

            map[Spider.LINK] = webDriver.currentUrl
            map[Spider.TITLE] = webDriver.title
            map[Spider.USER] = webDriver.findElement(By.cssSelector("div.index-module_authorTxt_V6XfG>a>p")).text?:"百家号"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
            if(content!= null)
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK


    }
    private fun parseBaiNewsByPc(map: MutableMap<String, String?>) {

            val contentElem: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(ChromeDriverServiceWrapper.timeOut))
                .until { webDriver.findElement(By.cssSelector("div.index-module_articleWrap_2Zphx")) }

            map[Spider.LINK] = webDriver.currentUrl
            map[Spider.TITLE] = webDriver.title
            map[Spider.USER] = webDriver.findElement(By.cssSelector("p.index-module_authorName_7y5nA")).text?:"百家号"

            //map[Spider.BRIEF] = driver.findElement(By.cssSelector("meta[name=description]"))?.getAttribute("content")

            val content = contentElem.getAttribute("innerHTML")
            map[Spider.CONTENT] = content
//            if(content!= null)
//                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(content)?.firstOrNull()

            map[Spider.MSG] = "恭喜，解析成功"
            map[Spider.RET] = Spider.OK

    }
}

