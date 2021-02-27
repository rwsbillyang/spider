/*
 * Copyright © 2021 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2021-02-21 21:13
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

import com.github.rwsbillyang.spider.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException

//TODO: 文章图片不能展示，需特殊处理
class SpiderCM163: ISpider {
    private val log: Logger = LoggerFactory.getLogger("SpiderCM163")
    override val regPattern = "http(s)?://c\\.m\\.163\\.com/\\S+"
    override val errMsg = "请确认链接是否以开头： https://c.m.163.com/"

//    override val extractRules =
//        arrayOf(
//            ExtractRule(
//                Spider.TAG,
//                PrefixMatchRule("<meta name=\"keywords\"","content=\"","\"")
//            ),
//            ExtractRule(
//                Spider.BRIEF,
//                PrefixMatchRule("<meta name=\"description\"","content=\"","\"")
//            ),
//            ExtractRule(Spider.LINK, PrefixMatchRule("<link rel=\"canonical\"","href=\"","\"")),
//            ExtractRule(Spider.TITLE, PrefixMatchRule("<h1 class=\"header-title\"",">","<")),
//            //ExtractRule(Spider.IMGURL, PrefixMatchRule("<meta property=\"og:image\"","content=\"","\"")),
//
//
//            ExtractRule(
//                Spider.CONTENT, MultiLineRule(
//                    EqualRule("<div data-v-304061dd>"),
//                    EqualRule("</div>")
//                )
//            ),
//        )


    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        log.info("doParse, url=$url")
            try {
                val doc: Document =
                    Jsoup.connect(url).timeout(20 * 1000).userAgent(Spider.UAs_WX[Spider.UAs_WX.indices.random()]).followRedirects(true).get()

                map[Spider.TAG] = doc.select("meta[name=keywords]").attr("content")
                map[Spider.BRIEF] = doc.select("meta[name=description]").attr("content")
                map[Spider.TITLE] = doc.select("h1.header-title").text()
                map[Spider.USER] = doc.select("div.header-subtitle-info > .s-source").text()
                map[Spider.LINK] = doc.select("link[rel=canonical]").attr("href")?:url

                val text = doc.select("article > div > div[^data-v-]").html()
                map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(text)?.firstOrNull()
                map[Spider.CONTENT] = text

                map[Spider.RET] = Spider.OK
                map[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
            } catch (e: IOException) {
                e.printStackTrace()
                map[Spider.RET] = Spider.KO
                map[Spider.MSG] = "获取文章内容超时，请重试"
            }

        return map

    }
}

fun main(args: Array<String>) {
    //https://c.m.163.com/news/a/G3IIMQ340539QLK6.html?spss=newsapp
    SpiderCM163().doParse("https://c.m.163.com/news/a/G3MCJ3SK05199NPP.html")
        .forEach {
            println("${it.key}=${it.value}")
        }
}