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
import com.github.rwsbillyang.spider.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class Spider3G163: PageStreamParser(Spider.UAs_WX), ISpider {
    override val regPattern = "http(s)?://(3g|news|www)\\.163\\.com/\\S+"
    override val errMsg = "请确认链接是否以开头： https://3g.163.com/"

    override val extractRules =
        arrayOf(
            ExtractRule(Spider.TITLE, ContainMatchRule("<meta property=\"og:title\"","content=\"","_"),StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.USER, PrefixMatchRule("<meta property=\"article:author\"","content=\"","\""),StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.TAG, PrefixMatchRule("<meta property=\"article:tag\"","content=\"","\""),StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.BRIEF,PrefixMatchRule("<meta property=\"og:description\"","content=\"","\""),StartIndexHint.AfterMatchStrIndex),
            //ExtractRule(Spider.IMGURL, PrefixMatchRule("<meta property=\"og:image\"","content=\"","\""),StartIndexHint.AfterMatchStrIndex),
            ExtractRule(Spider.CONTENT, MultiLineRule(PrefixRule("<article "), EqualRule("</article>"))),
        )


    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        getPageAndParse(url, map)
        map[Spider.IMGURL] = HtmlImgUtil.getImageSrc2(map[Spider.CONTENT])?.firstOrNull()
        map[Spider.LINK] = url

        return map

    }


    //it's also works well
     fun doParse2(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()

        try {
            val doc: Document =
                Jsoup.connect(url).timeout(20 * 1000).userAgent(Spider.UAs_PC[Spider.UAs_PC.indices.random()])
                    .followRedirects(true).get()
            map[Spider.TITLE] = doc.select("h1.title").text()
            map[Spider.IMGURL] = doc.select("meta[property=og:image]").attr("content")
            map[Spider.TAG] = doc.select("meta[property=article:tag]").attr("content")
            map[Spider.BRIEF] = doc.select("meta[property=og:description]").attr("content")

            map[Spider.USER] = "网易新闻"
            map[Spider.LINK] = url.split("?").first()

            val text = doc.select("div.content > div.page").html()

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
