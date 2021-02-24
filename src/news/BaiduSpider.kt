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

import com.github.rwsbillyang.spider.ISpider
import com.github.rwsbillyang.spider.Spider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

//https://www.ruanyifeng.com/blog/2019/06/http-referer.html
class BaiduSpider : ISpider {
    private val log: Logger = LoggerFactory.getLogger("BaiduSpider")

    override val regPattern = "http(s)?://(\\w|-)+\\.baidu\\.com/\\S+"
    override val errMsg = "请使用 http://baijiahao.baidu.com/ 开头的文章链接上传"

    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        log.info("parse url=$url")
        try {
            val doc: Document =
                Jsoup.connect(url).timeout(20 * 1000).userAgent(Spider.UAs[0]).followRedirects(true).get()

            //https://m.toutiao.com/i6931886311808827912/info/v2/
            var text: String = doc.select("title").text()
            map[Spider.TITLE] = text

//            text = doc.select("meta[name=description]").attr("content")
//            map[Spider.BRIEF] = text


            map[Spider.USER] = doc.select("span.account-authentication").text()

            val es: Elements = doc.select("div.article-content")
            if(es.size > 0){
                text = es[0].html()
                map[Spider.CONTENT] = text

                //图片地址为空，从正文内容中寻找第一张
                map[Spider.IMGURL] = doc.select("div.author-icon > a > img").attr("src")?:HtmlImgUtil.getImageSrc(text)?.firstOrNull()
            }


            map[Spider.LINK] = url
            //map[Spider.USER] = "百家号"

            map[Spider.RET] = Spider.OK
            map[Spider.MSG] = "恭喜，解析成功，请编辑保存！"
            return map
        } catch (e: IOException) {
            e.printStackTrace()
            map[Spider.RET] = Spider.KO
            map[Spider.MSG] = "获取文章内容超时，请重试"
        }

        return map
    }
}

fun main(args: Array<String>) {
    BaiduSpider().doParse("http://baijiahao.baidu.com/s?id=1692558786334703012")
        .forEach {
            println("${it.key}=${it.value}")
        }
}