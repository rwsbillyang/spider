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

class Spider163: PageStreamParser(), ISpider {
    override val regPattern = "http(s)?://3g\\.163\\.com/\\S+"
    override val errMsg = "请确认链接是否以开头： https://3g.163.com/"

    override val extractRules =
        arrayOf(
            ExtractRule(Spider.TITLE, PrefixMatchRule("<h1 class=\"title\"",">","<")),
            ExtractRule(Spider.IMGURL, PrefixMatchRule("<meta property=\"og:image\"","content=\"","\"")),
            ExtractRule(
                Spider.BRIEF,
                PrefixMatchRule("<meta property=\"og:description\"","content=\"","\">")
            ),
            ExtractRule(
                Spider.CONTENT, MultiLineRule(
                    EqualRule("<div class=\"page js-page on\">"),
                    EqualRule("<div class=\"otitle_editor\">")
                )
            ),
        )

    //https://3g.163.com/all/article/DB8SPSIU0001875P.html
    //https://3g.163.com/all/article/DB85P66L0001899O.html
    override fun doParse(url: String): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        getPageAndParse(url, map)
        map[Spider.LINK] = url
        map[Spider.USER] = "网易"

        return map

    }
}

