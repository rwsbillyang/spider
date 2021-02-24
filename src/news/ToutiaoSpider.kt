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
import org.jsoup.select.Elements
import java.io.IOException



//网页html全部在一行，PageStreamParser已不适用
class ToutiaoSpider: PageStreamParser(), ISpider {
    override val regPattern = "http(s)?://(m|www)\\.toutiao\\.com/\\S+"
    override val errMsg = "请确认链接是否以开头： https://m.toutiao.com/ 或 https://www.toutiao.com/"

    override val extractRules =
        arrayOf(
            ExtractRule(Spider.TITLE, PrefixMatchRule("title:","'","'")),
                ExtractRule(
                        Spider.BRIEF,
                        ContainMatchRule("<meta name=description content","<meta name=description content=",">")
                ),
            ExtractRule(Spider.CONTENT, PrefixMatchRule("content:","'","'"))
        )



    //https://www.toutiao.com/i6525188057665110531/
    //https://m.toutiao.com/i6525188057665110531/
    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()

        val url2=url.replace("//m.", "//www.");
//        getPageAndParse(url2, map)
//        map.put(Spider.LINK, url2)
//        map.put(Spider.USER, "今日头条")
        doParseBasedInJsoup(url2, map)

        return map
    }


    //https://www.toutiao.com/a6931886311808827912/
    //https://m.toutiao.com/i6931886311808827912/info/v2/
    private fun doParseBasedInJsoup(url: String, map: MutableMap<String, String?>) {
        try {
            val doc: Document =
                Jsoup.connect(url).timeout(20 * 1000).userAgent(Spider.UAs[0]).followRedirects(true).get()
            var text: String = doc.select("title").text()
            map[Spider.TITLE] = text

            text = doc.select("meta[name=description]").attr("content")
            map[Spider.BRIEF] = text

//            text = doc.select("meta[property=og:image]").attr("content")
//            map[Spider.IMGURL] = text

            val es: Elements = doc.select("article")
            if(es.size > 0){
                text = es[0].html()
                map[Spider.CONTENT] = text

                //图片地址为空，从正文内容中寻找第一张
                val list = HtmlImgUtil.getImageSrc2(text)
                if (!list.isNullOrEmpty()) {
                    map[Spider.IMGURL] = list[0]
                }
            }


            map[Spider.LINK] = url
            map[Spider.USER] = "今日头条"

            map[Spider.RET] = Spider.OK
            map[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
        } catch (e: IOException) {
            e.printStackTrace()
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "获取文章内容超时，请重试"
        }
    }
}

