/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-09-17 10:22
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

package com.github.rwsbillyang.spider.deprecated

import com.github.rwsbillyang.spider.Spider
import com.github.rwsbillyang.spider.WebDriverClient
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/**
 * @Deprecate("ToutiaoSpider也支持视频")
 * */
//https://m.toutiaoimg.cn/i6912100384953598475/
class ToutiaoVideoSpider(uaIndex: Int = Spider.UAs_WX) : WebDriverClient(uaIndex) {

    override val regPattern = "http(s)?://(m|www)\\.toutiaoimg\\.(com|cn)/(a|i)\\S+"
    override val errMsg = "链接中需有这些字符：toutiaoimg.cn"


    override fun extract(url: String, map: MutableMap<String, String?>) {
        val video: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(30))
            .until { webDriver.findElement(By.tagName("video")) }

        map[Spider.LINK] = url.split("?").first()

        map[Spider.VIDEO] =
            video.findElements(By.tagName("source"))?.firstOrNull()?.getAttribute("src")?.split("?")?.first()
        map[Spider.TITLE] = webDriver.findElement(By.cssSelector("h1.video-title"))?.text

        webDriver.findElement(By.cssSelector("div.video-title-unfold")).let {
            it.click() //点击后才出现author信息
            val author: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(10))
                .until { webDriver.findElement(By.cssSelector("div.author")) }
            map[Spider.USER] = author.findElement(By.cssSelector("div.author-info-name"))?.text ?: "头条"
            map[Spider.IMGURL] = author.findElement(By.cssSelector("img.author-avatar-img"))?.getAttribute("src")
        }


        map[Spider.MSG] = "恭喜，解析成功"
        map[Spider.RET] = Spider.OK
    }
}

