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

//TODO: 里面的视频有问题
class Spider163: PageStreamParser(Spider.UAs_PC), ISpider {
    override val regPattern = "http(s)?://(news|www)?\\.?163\\.com/\\S+"
    override val errMsg = "请确认链接是否以开头： https://3g.163.com/"

    override val extractRules =
        arrayOf(
            ExtractRule(
                Spider.TAG,
                PrefixMatchRule("<meta name=\"keywords\"","content=\"","\"")
            ),
            ExtractRule(Spider.TITLE, PrefixMatchRule("<meta property=\"og:title\"","content=\"","\"")),
            ExtractRule(
                Spider.BRIEF,
                PrefixMatchRule("<meta property=\"og:description\"","content=\"","\"")
            ),
            ExtractRule(Spider.LINK, PrefixMatchRule("<link rel=\"canonical\"","href=\"","\"")),
            ExtractRule(Spider.IMGURL, PrefixMatchRule("<meta property=\"og:image\"","content=\"","\"")),


            ExtractRule(
                Spider.CONTENT, MultiLineRule(
                    EqualRule("<div class=\"post_body\">"),
                    EqualRule("</div>")
                )
            ),
        )


    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        if(url.contains("//www."))
            getPageAndParse(url, map, "GBK")
        else getPageAndParse(url, map)

        if(map[Spider.LINK].isNullOrBlank())
            map[Spider.LINK] = url.split("?").firstOrNull()
        if(map[Spider.IMGURL].isNullOrBlank())
            map[Spider.IMGURL] = HtmlImgUtil.getImageSrc(map[Spider.CONTENT])?.firstOrNull()
        map[Spider.USER] = "网易"

        return map

    }
}

fun main(args: Array<String>) {
    //https://www.163.com/dy/article/G3M3MP6G0534P59R.html?clickfrom=w_yw
    //https://news.163.com/21/0225/11/G3M7QPGP0001899O.html?clickfrom=w_yw
    Spider163().doParse("https://news.163.com/21/0225/11/G3M7QPGP0001899O.html?clickfrom=w_yw")
        .forEach {
            println("${it.key}=${it.value}")
        }
}